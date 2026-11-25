import React, { useEffect, useMemo, useRef, useState } from 'react';
import { onAuthStateChanged, signInWithPopup, signOut, type User } from 'firebase/auth';
import {
  ArrowDownLeft,
  ArrowUpRight,
  Cloud,
  CloudOff,
  Download,
  FileDown,
  FileSpreadsheet,
  Pencil,
  Plus,
  Printer,
  RefreshCw,
  Search,
  Trash2,
  Upload,
} from 'lucide-react';
import { ClientModal, ProjectModal } from './components/EntityModals';
import { EntryModal } from './components/EntryModal';
import {
  buildCsvLedger,
  buildJsonBackup,
  loadLocalStore,
  loadSavedLanguage,
  parseJsonBackup,
  saveLanguage,
  saveLocalStore,
} from './data/storage';
import { auth, googleProvider } from './firebase';
import {
  AppLanguage,
  categoryLabel,
  getStrings,
  paymentModeLabel,
  projectStatusLabel,
} from './i18n/strings';
import { fetchCloudBackupSnapshot, syncLocalDataToCloud } from './services/cloudSync';
import {
  AccountCategory,
  Client,
  LedgerTransaction,
  Project,
  SyncStatus,
  TransactionType,
} from './types/accounts';
import {
  formatDate,
  formatFileStamp,
  formatInr,
  formatQty,
  isToday,
  isYesterday,
  startOfMonthMillis,
} from './utils/formatters';
import {
  downloadPaymentReceiptPdf,
  downloadProfitLossPdf,
  downloadProjectInvoicePdf,
} from './utils/pdfGenerator';

type MainView = 'overview' | 'projects' | 'accounts' | 'clients' | 'reports';
type AccountsSubTab = 'all' | 'materials' | 'labour' | 'site';
type DateRange = 'all' | 'month' | '30d';

export default function App() {
  const [lang, setLang] = useState<AppLanguage>(() => loadSavedLanguage());
  const s = useMemo(() => getStrings(lang), [lang]);

  const initialStore = useMemo(() => loadLocalStore(), []);
  const [clients, setClients] = useState<Client[]>(initialStore.clients);
  const [projects, setProjects] = useState<Project[]>(initialStore.projects);
  const [transactions, setTransactions] = useState<LedgerTransaction[]>(
    initialStore.transactions
  );

  const [view, setView] = useState<MainView>('overview');
  const [accountsSubTab, setAccountsSubTab] = useState<AccountsSubTab>('all');
  const [selectedProjectFilter, setSelectedProjectFilter] = useState<string>('all');
  const [selectedProjectIdDetail, setSelectedProjectIdDetail] = useState<string | null>(null);
  const [selectedClientIdDetail, setSelectedClientIdDetail] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [dateRange, setDateRange] = useState<DateRange>('all');

  const [isOnline, setIsOnline] = useState<boolean>(
    typeof navigator !== 'undefined' ? navigator.onLine : true
  );
  const [user, setUser] = useState<User | null>(null);
  const [isSyncing, setIsSyncing] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  // Modals
  const [entryModalState, setEntryModalState] = useState<{
    open: boolean;
    existing: LedgerTransaction | null;
    initialType: TransactionType;
    initialProjectId?: string;
    initialClientId?: string;
  }>({
    open: false,
    existing: null,
    initialType: TransactionType.EXPENSE_MATERIAL,
  });

  const [projectModalState, setProjectModalState] = useState<{
    open: boolean;
    existing: Project | null;
  }>({ open: false, existing: null });

  const [clientModalState, setClientModalState] = useState<{
    open: boolean;
    existing: Client | null;
  }>({ open: false, existing: null });

  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const showToast = (msg: string) => {
    setToast(msg);
    window.setTimeout(() => setToast(null), 3600);
  };

  useEffect(() => {
    saveLocalStore({ seeded: true, clients, projects, transactions });
  }, [clients, projects, transactions]);

  useEffect(() => {
    saveLanguage(lang);
  }, [lang]);

  useEffect(() => {
    const onOnline = () => setIsOnline(true);
    const onOffline = () => setIsOnline(false);
    window.addEventListener('online', onOnline);
    window.addEventListener('offline', onOffline);
    return () => {
      window.removeEventListener('online', onOnline);
      window.removeEventListener('offline', onOffline);
    };
  }, []);

  useEffect(() => {
    const unsub = onAuthStateChanged(auth, (firebaseUser) => {
      setUser(firebaseUser);
    });
    return () => unsub();
  }, []);

  const projectMap = useMemo(() => new Map(projects.map((p) => [p.id, p])), [projects]);
  const clientMap = useMemo(() => new Map(clients.map((c) => [c.id, c])), [clients]);

  const sortedTransactions = useMemo(
    () => [...transactions].sort((a, b) => b.dateMillis - a.dateMillis || b.createdAt - a.createdAt),
    [transactions]
  );

  const projectSummaries = useMemo(() => {
    return projects.map((project) => {
      const pTx = sortedTransactions.filter((t) => t.projectId === project.id);
      const income = pTx
        .filter((t) => t.type === TransactionType.INCOME_CUSTOMER_PAYMENT)
        .reduce((acc, t) => acc + t.amount, 0);
      const expense = pTx
        .filter((t) => t.type !== TransactionType.INCOME_CUSTOMER_PAYMENT)
        .reduce((acc, t) => acc + t.amount, 0);
      const cement = pTx
        .filter((t) => t.category === AccountCategory.CEMENT)
        .reduce((acc, t) => acc + t.amount, 0);
      const steel = pTx
        .filter((t) => t.category === AccountCategory.STEEL)
        .reduce((acc, t) => acc + t.amount, 0);
      const sand = pTx
        .filter((t) => t.category === AccountCategory.SAND)
        .reduce((acc, t) => acc + t.amount, 0);
      const labour = pTx
        .filter((t) => t.type === TransactionType.EXPENSE_LABOUR)
        .reduce((acc, t) => acc + t.amount, 0);
      const site = pTx
        .filter((t) => t.type === TransactionType.EXPENSE_SITE)
        .reduce((acc, t) => acc + t.amount, 0);

      return {
        project,
        client: project.clientId ? clientMap.get(project.clientId) : undefined,
        transactions: pTx,
        income,
        expense,
        cement,
        steel,
        sand,
        labour,
        site,
        pending: Math.max(0, project.contractValue - income),
        net: income - expense,
      };
    });
  }, [projects, sortedTransactions, clientMap]);

  const clientSummaries = useMemo(() => {
    return clients.map((client) => {
      const cProjects = projects.filter((p) => p.clientId === client.id);
      const cProjectIds = new Set(cProjects.map((p) => p.id));
      const payments = sortedTransactions.filter(
        (t) =>
          t.type === TransactionType.INCOME_CUSTOMER_PAYMENT &&
          (t.clientId === client.id || cProjectIds.has(t.projectId))
      );
      const contractTotal = cProjects.reduce((acc, p) => acc + p.contractValue, 0);
      const paidTotal = payments.reduce((acc, t) => acc + t.amount, 0);
      return {
        client,
        projects: cProjects,
        payments,
        contractTotal,
        paidTotal,
        pending: Math.max(0, contractTotal - paidTotal),
      };
    });
  }, [clients, projects, sortedTransactions]);

  const overallTotals = useMemo(() => {
    const contract = projects.reduce((acc, p) => acc + p.contractValue, 0);
    const received = sortedTransactions
      .filter((t) => t.type === TransactionType.INCOME_CUSTOMER_PAYMENT)
      .reduce((acc, t) => acc + t.amount, 0);
    const expenses = sortedTransactions
      .filter((t) => t.type !== TransactionType.INCOME_CUSTOMER_PAYMENT)
      .reduce((acc, t) => acc + t.amount, 0);
    const pending = projectSummaries.reduce((acc, ps) => acc + ps.pending, 0);
    const unsynced =
      clients.filter((c) => c.syncStatus !== SyncStatus.SYNCED).length +
      projects.filter((p) => p.syncStatus !== SyncStatus.SYNCED).length +
      transactions.filter((t) => t.syncStatus !== SyncStatus.SYNCED).length;
    return { contract, received, expenses, net: received - expenses, pending, unsynced };
  }, [projects, sortedTransactions, projectSummaries, clients, transactions]);

  const filteredByScope = useMemo(() => {
    const now = Date.now();
    const cutoff =
      dateRange === 'month'
        ? startOfMonthMillis()
        : dateRange === '30d'
          ? now - 30 * 24 * 60 * 60 * 1000
          : 0;
    const q = searchQuery.trim().toLowerCase();
    return sortedTransactions.filter((t) => {
      if (selectedProjectFilter !== 'all' && t.projectId !== selectedProjectFilter) return false;
      if (view === 'reports' && t.dateMillis < cutoff) return false;
      if (q) {
        const match =
          t.title.toLowerCase().includes(q) ||
          t.partyName.toLowerCase().includes(q) ||
          t.referenceNo.toLowerCase().includes(q) ||
          categoryLabel(t.category, s).toLowerCase().includes(q);
        if (!match) return false;
      }
      return true;
    });
  }, [sortedTransactions, selectedProjectFilter, dateRange, view, searchQuery, s]);

  const accountsTabRows = useMemo(() => {
    if (accountsSubTab === 'materials') {
      return filteredByScope.filter((t) => t.type === TransactionType.EXPENSE_MATERIAL);
    }
    if (accountsSubTab === 'labour') {
      return filteredByScope.filter((t) => t.type === TransactionType.EXPENSE_LABOUR);
    }
    if (accountsSubTab === 'site') {
      return filteredByScope.filter((t) => t.type === TransactionType.EXPENSE_SITE);
    }
    return filteredByScope;
  }, [filteredByScope, accountsSubTab]);

  const materialMetrics = useMemo(() => {
    const matRows = filteredByScope.filter((t) => t.type === TransactionType.EXPENSE_MATERIAL);
    const list = [
      { category: AccountCategory.CEMENT, unit: s.bagsUnit },
      { category: AccountCategory.STEEL, unit: s.kgUnit },
      { category: AccountCategory.SAND, unit: s.unitsLoad },
    ];
    return list.map(({ category, unit }) => {
      const rows = matRows.filter((r) => r.category === category);
      return {
        category,
        unit,
        spend: rows.reduce((sum, r) => sum + r.amount, 0),
        qty: rows.reduce((sum, r) => sum + (r.quantity || 0), 0),
        count: rows.length,
      };
    });
  }, [filteredByScope, s]);

  const upsertTransaction = (tx: LedgerTransaction) => {
    setTransactions((prev) => {
      const exists = prev.some((item) => item.id === tx.id);
      return exists ? prev.map((item) => (item.id === tx.id ? tx : item)) : [tx, ...prev];
    });
  };

  const deleteTransaction = (id: string) => {
    setTransactions((prev) => prev.filter((t) => t.id !== id));
  };

  const upsertProject = (proj: Project) => {
    setProjects((prev) => {
      const exists = prev.some((item) => item.id === proj.id);
      return exists ? prev.map((item) => (item.id === proj.id ? proj : item)) : [proj, ...prev];
    });
  };

  const deleteProject = (id: string) => {
    setProjects((prev) => prev.filter((p) => p.id !== id));
    setTransactions((prev) => prev.filter((t) => t.projectId !== id));
    if (selectedProjectIdDetail === id) setSelectedProjectIdDetail(null);
    if (selectedProjectFilter === id) setSelectedProjectFilter('all');
  };

  const upsertClient = (client: Client) => {
    setClients((prev) => {
      const exists = prev.some((item) => item.id === client.id);
      return exists ? prev.map((item) => (item.id === client.id ? client : item)) : [client, ...prev];
    });
  };

  const deleteClient = (id: string) => {
    setClients((prev) => prev.filter((c) => c.id !== id));
    setProjects((prev) => prev.map((p) => (p.clientId === id ? { ...p, clientId: '' } : p)));
    if (selectedClientIdDetail === id) setSelectedClientIdDetail(null);
  };

  const handleSignInAndSync = async () => {
    try {
      setIsSyncing(true);
      const cred = await signInWithPopup(auth, googleProvider);
      const uid = cred.user.uid;
      if (transactions.length > 0 || projects.length > 0 || clients.length > 0) {
        await syncLocalDataToCloud(uid, clients, projects, transactions);
        setClients((prev) => prev.map((c) => ({ ...c, syncStatus: SyncStatus.SYNCED })));
        setProjects((prev) => prev.map((p) => ({ ...p, syncStatus: SyncStatus.SYNCED })));
        setTransactions((prev) => prev.map((t) => ({ ...t, syncStatus: SyncStatus.SYNCED })));
      } else {
        const cloud = await fetchCloudBackupSnapshot(uid);
        if (cloud.projects.length > 0 || cloud.clients.length > 0) {
          setClients(cloud.clients);
          setProjects(cloud.projects);
          setTransactions(cloud.transactions);
        }
      }
      showToast(s.syncSuccess);
    } catch {
      showToast(s.syncFailed);
    } finally {
      setIsSyncing(false);
    }
  };

  const handleManualSync = async () => {
    if (!user) {
      await handleSignInAndSync();
      return;
    }
    try {
      setIsSyncing(true);
      await syncLocalDataToCloud(user.uid, clients, projects, transactions);
      setClients((prev) => prev.map((c) => ({ ...c, syncStatus: SyncStatus.SYNCED })));
      setProjects((prev) => prev.map((p) => ({ ...p, syncStatus: SyncStatus.SYNCED })));
      setTransactions((prev) => prev.map((t) => ({ ...t, syncStatus: SyncStatus.SYNCED })));
      showToast(s.syncSuccess);
    } catch {
      showToast(s.syncFailed);
    } finally {
      setIsSyncing(false);
    }
  };

  const handleDownloadJson = () => {
    const content = buildJsonBackup({ seeded: true, clients, projects, transactions });
    const blob = new Blob([content], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `CholaEmpire_Backup_${formatFileStamp()}.json`;
    a.click();
    URL.revokeObjectURL(url);
    showToast(s.backupExportedSuccess);
  };

  const handleDownloadCsv = () => {
    const csv = buildCsvLedger(sortedTransactions, projects, clients);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `CholaEmpire_Ledger_${formatFileStamp()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    showToast(s.csvExportedSuccess);
  };

  const handleRestoreFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    try {
      const text = await file.text();
      const payload = parseJsonBackup(text);
      if (window.confirm(s.confirmRestoreMessage)) {
        setClients(payload.clients);
        setProjects(payload.projects);
        setTransactions(payload.transactions);
        showToast(s.restoreSuccess);
      }
    } catch {
      showToast(s.restoreFailed);
    }
  };

  const handleReceiptPdf = (tx: LedgerTransaction) => {
    const proj = projectMap.get(tx.projectId);
    const cli = clientMap.get(tx.clientId || proj?.clientId || '');
    const totalPaid = sortedTransactions
      .filter(
        (t) =>
          t.projectId === tx.projectId && t.type === TransactionType.INCOME_CUSTOMER_PAYMENT
      )
      .reduce((acc, t) => acc + t.amount, 0);
    downloadPaymentReceiptPdf(tx, proj, cli, totalPaid, s);
  };

  const handleProjectInvoicePdf = (projectId: string) => {
    const proj = projectMap.get(projectId);
    if (!proj) return;
    const cli = proj.clientId ? clientMap.get(proj.clientId) : undefined;
    const pTx = sortedTransactions.filter((t) => t.projectId === proj.id);
    downloadProjectInvoicePdf(proj, cli, pTx, s);
  };

  const pnlStats = useMemo(() => {
    const income = filteredByScope
      .filter((t) => t.type === TransactionType.INCOME_CUSTOMER_PAYMENT)
      .reduce((acc, t) => acc + t.amount, 0);
    const cement = filteredByScope
      .filter((t) => t.category === AccountCategory.CEMENT)
      .reduce((acc, t) => acc + t.amount, 0);
    const steel = filteredByScope
      .filter((t) => t.category === AccountCategory.STEEL)
      .reduce((acc, t) => acc + t.amount, 0);
    const sand = filteredByScope
      .filter((t) => t.category === AccountCategory.SAND)
      .reduce((acc, t) => acc + t.amount, 0);
    const otherMaterial = filteredByScope
      .filter(
        (t) =>
          t.type === TransactionType.EXPENSE_MATERIAL &&
          ![AccountCategory.CEMENT, AccountCategory.STEEL, AccountCategory.SAND].includes(
            t.category
          )
      )
      .reduce((acc, t) => acc + t.amount, 0);
    const labour = filteredByScope
      .filter((t) => t.type === TransactionType.EXPENSE_LABOUR)
      .reduce((acc, t) => acc + t.amount, 0);
    const site = filteredByScope
      .filter((t) => t.type === TransactionType.EXPENSE_SITE)
      .reduce((acc, t) => acc + t.amount, 0);
    const totalExpense = cement + steel + sand + otherMaterial + labour + site;
    const net = income - totalExpense;
    const margin = income > 0 ? (net / income) * 100 : 0;
    return {
      income,
      cement,
      steel,
      sand,
      otherMaterial,
      labour,
      site,
      totalExpense,
      net,
      margin,
    };
  }, [filteredByScope]);

  const navItems: { id: MainView; label: string }[] = [
    { id: 'overview', label: s.navOverview },
    { id: 'projects', label: s.navProjects },
    { id: 'accounts', label: s.navAccounts },
    { id: 'clients', label: s.navClients },
    { id: 'reports', label: s.navReports },
  ];

  const selectedProjectDetail = projectSummaries.find(
    (p) => p.project.id === selectedProjectIdDetail
  );
  const selectedClientDetail = clientSummaries.find(
    (c) => c.client.id === selectedClientIdDetail
  );

  return (
    <div className="min-h-screen bg-[#f7f5f0] text-zinc-900 flex flex-col">
      {/* 3-Zone Top Bar Contract */}
      <header className="bg-white border-b border-[#e2ddd2] sticky top-0 z-30 print:hidden">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between gap-8">
          <a
            href="#overview"
            onClick={(e) => {
              e.preventDefault();
              setView('overview');
              setSelectedProjectIdDetail(null);
              setSelectedClientIdDetail(null);
            }}
            className="text-lg font-bold tracking-tight text-[#9a3412] whitespace-nowrap shrink-0"
          >
            {s.brandTitle}
          </a>

          <nav className="hidden md:flex items-center gap-6 text-sm font-medium">
            {navItems.map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => {
                  setView(item.id);
                  setSelectedProjectIdDetail(null);
                  setSelectedClientIdDetail(null);
                }}
                className={`whitespace-nowrap shrink-0 py-1 border-b-2 transition-colors ${
                  view === item.id
                    ? 'border-[#9a3412] text-zinc-900'
                    : 'border-transparent text-zinc-600 hover:text-zinc-900'
                }`}
              >
                {item.label}
              </button>
            ))}
          </nav>

          <button
            type="button"
            disabled={projects.length === 0}
            onClick={() =>
              setEntryModalState({
                open: true,
                existing: null,
                initialType: TransactionType.EXPENSE_MATERIAL,
                initialProjectId:
                  selectedProjectFilter !== 'all' ? selectedProjectFilter : projects[0]?.id,
              })
            }
            className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-white bg-[#9a3412] rounded-lg hover:bg-[#7c2d12] transition-colors whitespace-nowrap shrink-0 disabled:opacity-50"
          >
            <Plus className="w-4 h-4" />
            {s.quickEntry}
          </button>
        </div>
      </header>

      {/* Workspace Utility Strip (below top bar) */}
      <div className="bg-[#efece4] border-b border-[#e2ddd2] print:hidden">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-2.5 flex flex-wrap items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-3 text-zinc-600">
            <span>{s.brandSubtitle}</span>
            <span aria-hidden="true">·</span>
            <span className="inline-flex items-center gap-1.5 font-medium text-zinc-800">
              {isOnline ? <Cloud className="w-3.5 h-3.5 text-[#0f766e]" /> : <CloudOff className="w-3.5 h-3.5 text-amber-700" />}
              {isOnline ? s.onlineStatus : s.offlineStatus}
            </span>
          </div>

          <div className="flex items-center gap-3">
            <div className="md:hidden flex items-center gap-1 bg-white border border-[#e2ddd2] rounded-lg p-0.5 overflow-x-auto max-w-full">
              {navItems.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => setView(item.id)}
                  className={`px-2.5 py-1 text-xs font-medium rounded-md whitespace-nowrap shrink-0 ${
                    view === item.id ? 'bg-[#9a3412] text-white' : 'text-zinc-600'
                  }`}
                >
                  {item.label}
                </button>
              ))}
            </div>

            <div className="flex items-center gap-1 bg-white border border-[#e2ddd2] rounded-lg p-0.5">
              <button
                type="button"
                onClick={() => setLang('en')}
                className={`px-2.5 py-1 text-xs font-medium rounded-md transition-colors whitespace-nowrap shrink-0 ${
                  lang === 'en' ? 'bg-[#9a3412] text-white' : 'text-zinc-600 hover:text-zinc-900'
                }`}
              >
                English
              </button>
              <button
                type="button"
                onClick={() => setLang('ta')}
                className={`px-2.5 py-1 text-xs font-medium rounded-md transition-colors whitespace-nowrap shrink-0 ${
                  lang === 'ta' ? 'bg-[#9a3412] text-white' : 'text-zinc-600 hover:text-zinc-900'
                }`}
              >
                தமிழ்
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 py-6 space-y-6">
        {/* OVERVIEW */}
        {view === 'overview' && (
          <>
            <section className="bg-white border border-[#e2ddd2] rounded-xl p-6">
              <div className="grid grid-cols-2 lg:grid-cols-5 gap-6">
                <div>
                  <p className="text-xs font-medium text-zinc-500">{s.netProfitLoss}</p>
                  <p
                    className={`mt-1 text-2xl font-bold font-mono-num ${
                      overallTotals.net >= 0 ? 'text-[#0f766e]' : 'text-[#b91c1c]'
                    }`}
                  >
                    {formatInr(overallTotals.net)}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium text-zinc-500">{s.totalReceived}</p>
                  <p className="mt-1 text-xl font-semibold font-mono-num text-[#0f766e]">
                    {formatInr(overallTotals.received)}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium text-zinc-500">{s.totalExpenses}</p>
                  <p className="mt-1 text-xl font-semibold font-mono-num text-[#b91c1c]">
                    {formatInr(overallTotals.expenses)}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium text-zinc-500">{s.totalContract}</p>
                  <p className="mt-1 text-xl font-semibold font-mono-num text-zinc-900">
                    {formatInr(overallTotals.contract)}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium text-zinc-500">{s.pendingBalance}</p>
                  <p className="mt-1 text-xl font-semibold font-mono-num text-[#b45309]">
                    {formatInr(overallTotals.pending)}
                  </p>
                </div>
              </div>
            </section>

            {/* Quick Entry Actions */}
            <section className="grid grid-cols-2 lg:grid-cols-4 gap-3">
              {[
                {
                  type: TransactionType.INCOME_CUSTOMER_PAYMENT,
                  label: s.addCustomerPayment,
                },
                {
                  type: TransactionType.EXPENSE_MATERIAL,
                  label: s.addMaterial,
                },
                {
                  type: TransactionType.EXPENSE_LABOUR,
                  label: s.addLabour,
                },
                {
                  type: TransactionType.EXPENSE_SITE,
                  label: s.addSiteExpense,
                },
              ].map((action) => (
                <button
                  key={action.type}
                  type="button"
                  disabled={projects.length === 0}
                  onClick={() =>
                    setEntryModalState({
                      open: true,
                      existing: null,
                      initialType: action.type,
                      initialProjectId: projects[0]?.id,
                    })
                  }
                  className="flex items-center justify-between px-4 py-3.5 bg-white border border-[#e2ddd2] rounded-xl text-left hover:border-[#9a3412] transition-colors disabled:opacity-50"
                >
                  <span className="text-sm font-medium text-zinc-900">{action.label}</span>
                  <Plus className="w-4 h-4 text-[#9a3412] shrink-0" />
                </button>
              ))}
            </section>

            {/* Active Projects */}
            <section className="space-y-3">
              <div className="flex items-center justify-between">
                <h2 className="text-base font-semibold text-zinc-900">{s.activeProjects}</h2>
                <button
                  type="button"
                  onClick={() => setView('projects')}
                  className="text-xs font-medium text-[#9a3412] hover:underline"
                >
                  {s.navProjects}
                </button>
              </div>
              {projectSummaries.length === 0 ? (
                <div className="bg-white border border-[#e2ddd2] rounded-xl p-6 text-sm text-zinc-500">
                  {s.noProjectsYet}
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {projectSummaries.map((ps) => (
                    <div
                      key={ps.project.id}
                      onClick={() => {
                        setSelectedProjectIdDetail(ps.project.id);
                        setView('projects');
                      }}
                      className="bg-white border border-[#e2ddd2] rounded-xl p-5 cursor-pointer hover:border-[#9a3412] transition-colors space-y-3"
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <h3 className="text-base font-semibold text-zinc-900">{ps.project.name}</h3>
                          <p className="text-xs text-zinc-500 mt-0.5">
                            {[ps.client?.name, ps.project.location, projectStatusLabel(ps.project.status, s)]
                              .filter(Boolean)
                              .join(' · ')}
                          </p>
                        </div>
                        <div className="text-right">
                          <p className="text-xs text-zinc-500">{s.pendingBalance}</p>
                          <p className="text-sm font-semibold font-mono-num text-[#b45309]">
                            {formatInr(ps.pending)}
                          </p>
                        </div>
                      </div>
                      <div className="pt-3 border-t border-[#e2ddd2] grid grid-cols-3 gap-3 text-xs">
                        <div>
                          <span className="text-zinc-500 block">{s.income}</span>
                          <span className="font-semibold font-mono-num text-[#0f766e]">
                            {formatInr(ps.income)}
                          </span>
                        </div>
                        <div>
                          <span className="text-zinc-500 block">{s.expense}</span>
                          <span className="font-semibold font-mono-num text-[#b91c1c]">
                            {formatInr(ps.expense)}
                          </span>
                        </div>
                        <div className="text-right">
                          <span className="text-zinc-500 block">{s.netProfitLoss}</span>
                          <span
                            className={`font-semibold font-mono-num ${
                              ps.net >= 0 ? 'text-[#0f766e]' : 'text-[#b91c1c]'
                            }`}
                          >
                            {formatInr(ps.net)}
                          </span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>

            {/* Recent Ledger */}
            <section className="space-y-3">
              <h2 className="text-base font-semibold text-zinc-900">{s.recentTransactions}</h2>
              <TransactionTable
                rows={sortedTransactions.slice(0, 8)}
                projectMap={projectMap}
                strings={s}
                onEdit={(tx) =>
                  setEntryModalState({
                    open: true,
                    existing: tx,
                    initialType: tx.type,
                  })
                }
                onReceiptPdf={handleReceiptPdf}
              />
            </section>
          </>
        )}

        {/* PROJECTS */}
        {view === 'projects' && (
          <>
            {selectedProjectDetail ? (
              <div className="space-y-5">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <button
                      type="button"
                      onClick={() => setSelectedProjectIdDetail(null)}
                      className="text-xs font-medium text-[#9a3412] hover:underline mb-1"
                    >
                      ← {s.projectSummary}
                    </button>
                    <h2 className="text-xl font-bold text-zinc-900">
                      {selectedProjectDetail.project.name}
                    </h2>
                    <p className="text-xs text-zinc-500">
                      {[
                        selectedProjectDetail.client?.name,
                        selectedProjectDetail.project.location,
                        projectStatusLabel(selectedProjectDetail.project.status, s),
                      ]
                        .filter(Boolean)
                        .join(' · ')}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => handleProjectInvoicePdf(selectedProjectDetail.project.id)}
                      className="inline-flex items-center gap-1.5 px-3 py-2 text-xs font-medium bg-white border border-[#e2ddd2] rounded-lg hover:bg-[#efece4]"
                    >
                      <FileDown className="w-4 h-4 text-[#9a3412]" />
                      {s.generateInvoicePdf}
                    </button>
                    <button
                      type="button"
                      onClick={() =>
                        setProjectModalState({
                          open: true,
                          existing: selectedProjectDetail.project,
                        })
                      }
                      className="p-2 bg-white border border-[#e2ddd2] rounded-lg hover:bg-[#efece4]"
                      title={s.editProject}
                    >
                      <Pencil className="w-4 h-4 text-zinc-700" />
                    </button>
                    <button
                      type="button"
                      onClick={() => deleteProject(selectedProjectDetail.project.id)}
                      className="p-2 bg-white border border-[#e2ddd2] rounded-lg hover:bg-red-50"
                      title={s.delete}
                    >
                      <Trash2 className="w-4 h-4 text-[#b91c1c]" />
                    </button>
                  </div>
                </div>

                <div className="bg-white border border-[#e2ddd2] rounded-xl p-5 grid grid-cols-2 sm:grid-cols-5 gap-4">
                  <div>
                    <p className="text-xs text-zinc-500">{s.totalContract}</p>
                    <p className="text-lg font-semibold font-mono-num">
                      {formatInr(selectedProjectDetail.project.contractValue)}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-zinc-500">{s.totalReceived}</p>
                    <p className="text-lg font-semibold font-mono-num text-[#0f766e]">
                      {formatInr(selectedProjectDetail.income)}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-zinc-500">{s.pendingBalance}</p>
                    <p className="text-lg font-semibold font-mono-num text-[#b45309]">
                      {formatInr(selectedProjectDetail.pending)}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-zinc-500">{s.totalExpenses}</p>
                    <p className="text-lg font-semibold font-mono-num text-[#b91c1c]">
                      {formatInr(selectedProjectDetail.expense)}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-zinc-500">{s.netProfitLoss}</p>
                    <p
                      className={`text-lg font-semibold font-mono-num ${
                        selectedProjectDetail.net >= 0 ? 'text-[#0f766e]' : 'text-[#b91c1c]'
                      }`}
                    >
                      {formatInr(selectedProjectDetail.net)}
                    </p>
                  </div>
                </div>

                <div className="bg-white border border-[#e2ddd2] rounded-xl p-5 grid grid-cols-2 sm:grid-cols-5 gap-4 text-xs">
                  <div>
                    <span className="text-zinc-500 block">{s.cement}</span>
                    <span className="font-semibold font-mono-num text-sm">
                      {formatInr(selectedProjectDetail.cement)}
                    </span>
                  </div>
                  <div>
                    <span className="text-zinc-500 block">{s.steel}</span>
                    <span className="font-semibold font-mono-num text-sm">
                      {formatInr(selectedProjectDetail.steel)}
                    </span>
                  </div>
                  <div>
                    <span className="text-zinc-500 block">{s.sand}</span>
                    <span className="font-semibold font-mono-num text-sm">
                      {formatInr(selectedProjectDetail.sand)}
                    </span>
                  </div>
                  <div>
                    <span className="text-zinc-500 block">{s.tabLabour}</span>
                    <span className="font-semibold font-mono-num text-sm">
                      {formatInr(selectedProjectDetail.labour)}
                    </span>
                  </div>
                  <div>
                    <span className="text-zinc-500 block">{s.tabDailySite}</span>
                    <span className="font-semibold font-mono-num text-sm">
                      {formatInr(selectedProjectDetail.site)}
                    </span>
                  </div>
                </div>

                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() =>
                      setEntryModalState({
                        open: true,
                        existing: null,
                        initialType: TransactionType.INCOME_CUSTOMER_PAYMENT,
                        initialProjectId: selectedProjectDetail.project.id,
                        initialClientId: selectedProjectDetail.project.clientId,
                      })
                    }
                    className="px-4 py-2 text-xs font-semibold bg-[#0f766e] text-white rounded-lg"
                  >
                    + {s.recordPayment}
                  </button>
                  <button
                    type="button"
                    onClick={() =>
                      setEntryModalState({
                        open: true,
                        existing: null,
                        initialType: TransactionType.EXPENSE_MATERIAL,
                        initialProjectId: selectedProjectDetail.project.id,
                      })
                    }
                    className="px-4 py-2 text-xs font-semibold bg-[#9a3412] text-white rounded-lg"
                  >
                    + {s.addMaterial}
                  </button>
                </div>

                <TransactionTable
                  rows={selectedProjectDetail.transactions}
                  projectMap={projectMap}
                  strings={s}
                  onEdit={(tx) =>
                    setEntryModalState({ open: true, existing: tx, initialType: tx.type })
                  }
                  onReceiptPdf={handleReceiptPdf}
                />
              </div>
            ) : (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-xl font-bold text-zinc-900">{s.projectSummary}</h2>
                  <button
                    type="button"
                    onClick={() => setProjectModalState({ open: true, existing: null })}
                    className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-semibold bg-[#9a3412] text-white rounded-lg hover:bg-[#7c2d12]"
                  >
                    <Plus className="w-4 h-4" />
                    {s.addProject}
                  </button>
                </div>

                {projectSummaries.length === 0 ? (
                  <div className="bg-white border border-[#e2ddd2] rounded-xl p-8 text-sm text-zinc-500">
                    {s.noProjectsYet}
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {projectSummaries.map((ps) => (
                      <div
                        key={ps.project.id}
                        className="bg-white border border-[#e2ddd2] rounded-xl p-5 space-y-4"
                      >
                        <div className="flex items-start justify-between gap-3">
                          <div
                            onClick={() => setSelectedProjectIdDetail(ps.project.id)}
                            className="cursor-pointer flex-1"
                          >
                            <h3 className="text-base font-semibold text-zinc-900 hover:text-[#9a3412]">
                              {ps.project.name}
                            </h3>
                            <p className="text-xs text-zinc-500 mt-0.5">
                              {[ps.client?.name, ps.project.location, projectStatusLabel(ps.project.status, s)]
                                .filter(Boolean)
                                .join(' · ')}
                            </p>
                          </div>
                          <button
                            type="button"
                            onClick={() => handleProjectInvoicePdf(ps.project.id)}
                            className="inline-flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-[#9a3412] border border-[#e2ddd2] rounded-lg hover:bg-[#f7f5f0] whitespace-nowrap shrink-0"
                          >
                            <FileDown className="w-3.5 h-3.5" />
                            {s.generateInvoicePdf}
                          </button>
                        </div>

                        <div className="pt-3 border-t border-[#e2ddd2] grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
                          <div>
                            <span className="text-zinc-500 block">{s.totalContract}</span>
                            <span className="font-semibold font-mono-num">
                              {formatInr(ps.project.contractValue)}
                            </span>
                          </div>
                          <div>
                            <span className="text-zinc-500 block">{s.totalReceived}</span>
                            <span className="font-semibold font-mono-num text-[#0f766e]">
                              {formatInr(ps.income)}
                            </span>
                          </div>
                          <div>
                            <span className="text-zinc-500 block">{s.totalExpenses}</span>
                            <span className="font-semibold font-mono-num text-[#b91c1c]">
                              {formatInr(ps.expense)}
                            </span>
                          </div>
                          <div>
                            <span className="text-zinc-500 block">{s.pendingBalance}</span>
                            <span className="font-semibold font-mono-num text-[#b45309]">
                              {formatInr(ps.pending)}
                            </span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </>
        )}

        {/* ACCOUNTS (ALL LEDGER / MATERIALS / LABOUR / DAILY SITE) */}
        {view === 'accounts' && (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-1 bg-white border border-[#e2ddd2] rounded-lg p-1 overflow-x-auto">
                {(
                  [
                    ['all', s.tabAllLedger],
                    ['materials', s.tabMaterials],
                    ['labour', s.tabLabour],
                    ['site', s.tabDailySite],
                  ] as const
                ).map(([key, label]) => (
                  <button
                    key={key}
                    type="button"
                    onClick={() => setAccountsSubTab(key)}
                    className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors whitespace-nowrap shrink-0 ${
                      accountsSubTab === key
                        ? 'bg-[#9a3412] text-white'
                        : 'text-zinc-600 hover:text-zinc-900'
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>

              <div className="flex items-center gap-2 flex-wrap">
                <div className="relative">
                  <Search className="w-4 h-4 text-zinc-400 absolute left-3 top-2.5" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder={s.searchPlaceholder}
                    className="pl-9 pr-3 py-2 text-xs bg-white border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412] w-56"
                  />
                </div>
                <select
                  value={selectedProjectFilter}
                  onChange={(e) => setSelectedProjectFilter(e.target.value)}
                  className="px-3 py-2 text-xs bg-white border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
                >
                  <option value="all">{s.filterAllProjects}</option>
                  {projects.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {accountsSubTab === 'materials' && (
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                {materialMetrics.map((m) => (
                  <div key={m.category} className="bg-white border border-[#e2ddd2] rounded-xl p-4">
                    <p className="text-xs font-medium text-[#9a3412]">
                      {categoryLabel(m.category, s)}
                    </p>
                    <p className="mt-1 text-xl font-bold font-mono-num text-zinc-900">
                      {formatInr(m.spend)}
                    </p>
                    <p className="mt-1 text-xs text-zinc-500 font-mono-num">
                      {formatQty(m.qty)} {m.unit} · {m.count}
                    </p>
                  </div>
                ))}
              </div>
            )}

            {accountsSubTab === 'site' ? (
              <DailySiteGroupedList
                rows={accountsTabRows}
                projectMap={projectMap}
                strings={s}
                onEdit={(tx) =>
                  setEntryModalState({ open: true, existing: tx, initialType: tx.type })
                }
              />
            ) : (
              <TransactionTable
                rows={accountsTabRows}
                projectMap={projectMap}
                strings={s}
                onEdit={(tx) =>
                  setEntryModalState({ open: true, existing: tx, initialType: tx.type })
                }
                onReceiptPdf={handleReceiptPdf}
              />
            )}
          </div>
        )}

        {/* CLIENTS & CLIENT-WISE LEDGER */}
        {view === 'clients' && (
          <>
            {selectedClientDetail ? (
              <div className="space-y-5">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <button
                      type="button"
                      onClick={() => setSelectedClientIdDetail(null)}
                      className="text-xs font-medium text-[#9a3412] hover:underline mb-1"
                    >
                      ← {s.clientLedger}
                    </button>
                    <h2 className="text-xl font-bold text-zinc-900">
                      {selectedClientDetail.client.name}
                    </h2>
                    <p className="text-xs text-zinc-500">
                      {[selectedClientDetail.client.phone, selectedClientDetail.client.address]
                        .filter(Boolean)
                        .join(' · ')}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() =>
                        setEntryModalState({
                          open: true,
                          existing: null,
                          initialType: TransactionType.INCOME_CUSTOMER_PAYMENT,
                          initialProjectId: selectedClientDetail.projects[0]?.id,
                          initialClientId: selectedClientDetail.client.id,
                        })
                      }
                      className="px-4 py-2 text-xs font-semibold bg-[#0f766e] text-white rounded-lg"
                    >
                      + {s.recordPayment}
                    </button>
                    {selectedClientDetail.projects[0] && (
                      <button
                        type="button"
                        onClick={() =>
                          handleProjectInvoicePdf(selectedClientDetail.projects[0].id)
                        }
                        className="inline-flex items-center gap-1.5 px-3 py-2 text-xs font-medium bg-white border border-[#e2ddd2] rounded-lg"
                      >
                        <FileDown className="w-4 h-4 text-[#9a3412]" />
                        {s.generateInvoicePdf}
                      </button>
                    )}
                    <button
                      type="button"
                      onClick={() =>
                        setClientModalState({ open: true, existing: selectedClientDetail.client })
                      }
                      className="p-2 bg-white border border-[#e2ddd2] rounded-lg"
                    >
                      <Pencil className="w-4 h-4 text-zinc-700" />
                    </button>
                    <button
                      type="button"
                      onClick={() => deleteClient(selectedClientDetail.client.id)}
                      className="p-2 bg-white border border-[#e2ddd2] rounded-lg"
                    >
                      <Trash2 className="w-4 h-4 text-[#b91c1c]" />
                    </button>
                  </div>
                </div>

                <div className="bg-white border border-[#e2ddd2] rounded-xl p-5 grid grid-cols-3 gap-4">
                  <div>
                    <p className="text-xs text-zinc-500">{s.billedContract}</p>
                    <p className="text-lg font-semibold font-mono-num">
                      {formatInr(selectedClientDetail.contractTotal)}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-zinc-500">{s.receivedFromClient}</p>
                    <p className="text-lg font-semibold font-mono-num text-[#0f766e]">
                      {formatInr(selectedClientDetail.paidTotal)}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-zinc-500">{s.pendingBalance}</p>
                    <p className="text-lg font-semibold font-mono-num text-[#b45309]">
                      {formatInr(selectedClientDetail.pending)}
                    </p>
                  </div>
                </div>

                <TransactionTable
                  rows={selectedClientDetail.payments}
                  projectMap={projectMap}
                  strings={s}
                  onEdit={(tx) =>
                    setEntryModalState({ open: true, existing: tx, initialType: tx.type })
                  }
                  onReceiptPdf={handleReceiptPdf}
                />
              </div>
            ) : (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-xl font-bold text-zinc-900">{s.clientLedger}</h2>
                  <button
                    type="button"
                    onClick={() => setClientModalState({ open: true, existing: null })}
                    className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-semibold bg-[#9a3412] text-white rounded-lg hover:bg-[#7c2d12]"
                  >
                    <Plus className="w-4 h-4" />
                    {s.addClient}
                  </button>
                </div>

                {clientSummaries.length === 0 ? (
                  <div className="bg-white border border-[#e2ddd2] rounded-xl p-8 text-sm text-zinc-500">
                    {s.noClientsYet}
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {clientSummaries.map((cs) => (
                      <div
                        key={cs.client.id}
                        className="bg-white border border-[#e2ddd2] rounded-xl p-5 space-y-4"
                      >
                        <div className="flex items-start justify-between gap-3">
                          <div
                            onClick={() => setSelectedClientIdDetail(cs.client.id)}
                            className="cursor-pointer flex-1"
                          >
                            <h3 className="text-base font-semibold text-zinc-900 hover:text-[#9a3412]">
                              {cs.client.name}
                            </h3>
                            <p className="text-xs text-zinc-500 mt-0.5">
                              {[cs.client.phone, cs.projects.map((p) => p.name).join(', ')]
                                .filter(Boolean)
                                .join(' · ')}
                            </p>
                          </div>
                          <button
                            type="button"
                            onClick={() =>
                              setEntryModalState({
                                open: true,
                                existing: null,
                                initialType: TransactionType.INCOME_CUSTOMER_PAYMENT,
                                initialProjectId: cs.projects[0]?.id,
                                initialClientId: cs.client.id,
                              })
                            }
                            className="px-3 py-1.5 text-xs font-medium text-[#0f766e] border border-[#e2ddd2] rounded-lg hover:bg-[#f7f5f0] whitespace-nowrap shrink-0"
                          >
                            {s.recordPayment}
                          </button>
                        </div>

                        <div className="pt-3 border-t border-[#e2ddd2] grid grid-cols-3 gap-3 text-xs">
                          <div>
                            <span className="text-zinc-500 block">{s.billedContract}</span>
                            <span className="font-semibold font-mono-num">
                              {formatInr(cs.contractTotal)}
                            </span>
                          </div>
                          <div>
                            <span className="text-zinc-500 block">{s.receivedFromClient}</span>
                            <span className="font-semibold font-mono-num text-[#0f766e]">
                              {formatInr(cs.paidTotal)}
                            </span>
                          </div>
                          <div>
                            <span className="text-zinc-500 block">{s.pendingBalance}</span>
                            <span className="font-semibold font-mono-num text-[#b45309]">
                              {formatInr(cs.pending)}
                            </span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </>
        )}

        {/* REPORTS & BACKUP */}
        {view === 'reports' && (
          <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 className="text-xl font-bold text-zinc-900">{s.profitLossReport}</h2>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => window.print()}
                  className="inline-flex items-center gap-1.5 px-3 py-2 text-xs font-medium bg-white border border-[#e2ddd2] rounded-lg hover:bg-[#efece4] print:hidden"
                >
                  <Printer className="w-4 h-4" />
                  {s.printStatement}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    const scopeName =
                      selectedProjectFilter === 'all'
                        ? s.filterAllProjects
                        : projectMap.get(selectedProjectFilter)?.name ?? s.filterAllProjects;
                    const periodLabel =
                      dateRange === 'month'
                        ? s.dateRangeMonth
                        : dateRange === '30d'
                          ? s.dateRange30Days
                          : s.dateRangeAll;
                    downloadProfitLossPdf(
                      scopeName,
                      periodLabel,
                      pnlStats.income,
                      [
                        { label: s.cement, amount: pnlStats.cement },
                        { label: s.steel, amount: pnlStats.steel },
                        { label: s.sand, amount: pnlStats.sand },
                        { label: s.otherMaterial, amount: pnlStats.otherMaterial },
                        { label: s.tabLabour, amount: pnlStats.labour },
                        { label: s.tabDailySite, amount: pnlStats.site },
                      ],
                      s
                    );
                  }}
                  className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-semibold bg-[#9a3412] text-white rounded-lg hover:bg-[#7c2d12] print:hidden"
                >
                  <FileDown className="w-4 h-4" />
                  {s.exportReportPdf}
                </button>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-3 print:hidden">
              <div className="flex items-center gap-1 bg-white border border-[#e2ddd2] rounded-lg p-1">
                {(
                  [
                    ['all', s.dateRangeAll],
                    ['month', s.dateRangeMonth],
                    ['30d', s.dateRange30Days],
                  ] as const
                ).map(([val, label]) => (
                  <button
                    key={val}
                    type="button"
                    onClick={() => setDateRange(val)}
                    className={`px-3 py-1.5 text-xs font-medium rounded-md ${
                      dateRange === val ? 'bg-[#9a3412] text-white' : 'text-zinc-600'
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
              <select
                value={selectedProjectFilter}
                onChange={(e) => setSelectedProjectFilter(e.target.value)}
                className="px-3 py-2 text-xs bg-white border border-[#e2ddd2] rounded-lg"
              >
                <option value="all">{s.filterAllProjects}</option>
                {projects.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* P&L Statement */}
              <div className="bg-white border border-[#e2ddd2] rounded-xl p-6 space-y-5">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="text-xs font-medium text-zinc-500">
                      {pnlStats.net >= 0 ? s.profit : s.loss}
                    </p>
                    <p
                      className={`text-3xl font-bold font-mono-num mt-1 ${
                        pnlStats.net >= 0 ? 'text-[#0f766e]' : 'text-[#b91c1c]'
                      }`}
                    >
                      {formatInr(pnlStats.net)}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs font-medium text-zinc-500">{s.profitMargin}</p>
                    <p className="text-xl font-semibold font-mono-num mt-1">
                      {pnlStats.margin.toFixed(1)}%
                    </p>
                  </div>
                </div>

                <div className="pt-4 border-t border-[#e2ddd2] grid grid-cols-2 gap-4">
                  <div>
                    <p className="text-xs text-zinc-500">{s.income}</p>
                    <p className="text-lg font-semibold font-mono-num text-[#0f766e]">
                      {formatInr(pnlStats.income)}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-zinc-500">{s.expense}</p>
                    <p className="text-lg font-semibold font-mono-num text-[#b91c1c]">
                      {formatInr(pnlStats.totalExpense)}
                    </p>
                  </div>
                </div>

                <div className="pt-4 border-t border-[#e2ddd2] space-y-3">
                  <p className="text-sm font-semibold text-zinc-900">{s.expenseBreakdown}</p>
                  {[
                    { label: s.cement, amount: pnlStats.cement, color: 'bg-[#9a3412]' },
                    { label: s.steel, amount: pnlStats.steel, color: 'bg-slate-700' },
                    { label: s.sand, amount: pnlStats.sand, color: 'bg-amber-600' },
                    { label: s.otherMaterial, amount: pnlStats.otherMaterial, color: 'bg-zinc-500' },
                    { label: s.tabLabour, amount: pnlStats.labour, color: 'bg-[#b91c1c]' },
                    { label: s.tabDailySite, amount: pnlStats.site, color: 'bg-amber-700' },
                  ].map((item) => {
                    const pct =
                      pnlStats.totalExpense > 0
                        ? Math.round((item.amount / pnlStats.totalExpense) * 100)
                        : 0;
                    return (
                      <div key={item.label} className="space-y-1">
                        <div className="flex justify-between text-xs">
                          <span className="text-zinc-700">{item.label}</span>
                          <span className="font-mono-num font-medium">
                            {formatInr(item.amount)} ({pct}%)
                          </span>
                        </div>
                        <div className="h-2 bg-[#efece4] rounded-full overflow-hidden">
                          <div
                            className={`h-full ${item.color} rounded-full`}
                            style={{ width: `${pct}%` }}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Online Cloud Backup + Offline Entry + Data Export/Restore */}
              <div className="bg-white border border-[#e2ddd2] rounded-xl p-6 space-y-5 print:hidden">
                <div className="space-y-1.5">
                  <h3 className="text-base font-semibold text-zinc-900">{s.cloudBackupTitle}</h3>
                  <p className="text-xs text-zinc-600 leading-relaxed">{s.cloudBackupDesc}</p>
                  <p className="text-xs font-medium text-[#9a3412]">
                    {overallTotals.unsynced > 0
                      ? `${s.localQueueLabel}: ${overallTotals.unsynced}`
                      : s.allSyncedLabel}
                  </p>
                </div>

                {user ? (
                  <div className="p-3 bg-[#f7f5f0] border border-[#e2ddd2] rounded-lg flex flex-wrap items-center justify-between gap-2">
                    <div className="text-xs">
                      <span className="text-zinc-500 block">{s.signedInAs}</span>
                      <span className="font-semibold text-zinc-900">{user.email}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        disabled={isSyncing}
                        onClick={handleManualSync}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold bg-[#9a3412] text-white rounded-lg"
                      >
                        <RefreshCw className={`w-3.5 h-3.5 ${isSyncing ? 'animate-spin' : ''}`} />
                        {isSyncing ? s.syncing : s.syncNow}
                      </button>
                      <button
                        type="button"
                        onClick={() => signOut(auth)}
                        className="px-3 py-1.5 text-xs font-medium border border-[#e2ddd2] bg-white rounded-lg"
                      >
                        {s.signOutGoogle}
                      </button>
                    </div>
                  </div>
                ) : (
                  <button
                    type="button"
                    disabled={isSyncing}
                    onClick={handleSignInAndSync}
                    className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 text-xs font-semibold bg-[#9a3412] text-white rounded-lg hover:bg-[#7c2d12]"
                  >
                    <Cloud className="w-4 h-4" />
                    {isSyncing ? s.syncing : s.signInGoogleSync}
                  </button>
                )}

                <div className="pt-4 border-t border-[#e2ddd2] space-y-2.5">
                  <h4 className="text-sm font-semibold text-zinc-900">{s.backupAndRestore}</h4>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                    <button
                      type="button"
                      onClick={handleDownloadJson}
                      className="inline-flex items-center justify-center gap-2 px-3 py-2.5 text-xs font-medium bg-white border border-[#e2ddd2] rounded-lg hover:bg-[#f7f5f0]"
                    >
                      <Download className="w-4 h-4 text-[#9a3412]" />
                      {s.exportJsonBackup}
                    </button>
                    <button
                      type="button"
                      onClick={() => fileInputRef.current?.click()}
                      className="inline-flex items-center justify-center gap-2 px-3 py-2.5 text-xs font-medium bg-white border border-[#e2ddd2] rounded-lg hover:bg-[#f7f5f0]"
                    >
                      <Upload className="w-4 h-4 text-[#9a3412]" />
                      {s.restoreJsonBackup}
                    </button>
                  </div>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="application/json,.json"
                    onChange={handleRestoreFile}
                    className="hidden"
                  />
                  <button
                    type="button"
                    onClick={handleDownloadCsv}
                    className="w-full inline-flex items-center justify-center gap-2 px-3 py-2.5 text-xs font-medium bg-white border border-[#e2ddd2] rounded-lg hover:bg-[#f7f5f0]"
                  >
                    <FileSpreadsheet className="w-4 h-4 text-[#0f766e]" />
                    {s.exportCsvLedger}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      if (window.confirm(s.confirmClearMessage)) {
                        setClients([]);
                        setProjects([]);
                        setTransactions([]);
                      }
                    }}
                    className="w-full py-2 text-xs font-medium text-[#b91c1c] hover:bg-red-50 rounded-lg"
                  >
                    {s.clearAllData}
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Modals */}
      {entryModalState.open && (
        <EntryModal
          existing={entryModalState.existing}
          initialType={entryModalState.initialType}
          initialProjectId={entryModalState.initialProjectId}
          initialClientId={entryModalState.initialClientId}
          projects={projects}
          clients={clients}
          strings={s}
          onClose={() => setEntryModalState((prev) => ({ ...prev, open: false, existing: null }))}
          onSave={upsertTransaction}
          onDelete={deleteTransaction}
          onReceiptPdf={handleReceiptPdf}
        />
      )}

      {projectModalState.open && (
        <ProjectModal
          existing={projectModalState.existing}
          clients={clients}
          strings={s}
          onClose={() => setProjectModalState({ open: false, existing: null })}
          onSave={upsertProject}
        />
      )}

      {clientModalState.open && (
        <ClientModal
          existing={clientModalState.existing}
          strings={s}
          onClose={() => setClientModalState({ open: false, existing: null })}
          onSave={upsertClient}
        />
      )}

      {toast && (
        <div className="fixed bottom-5 right-5 z-50 px-4 py-2.5 bg-zinc-900 text-white text-xs font-medium rounded-lg shadow-lg">
          {toast}
        </div>
      )}
    </div>
  );
}

function TransactionTable({
  rows,
  projectMap,
  strings,
  onEdit,
  onReceiptPdf,
}: {
  rows: LedgerTransaction[];
  projectMap: Map<string, Project>;
  strings: ReturnType<typeof getStrings>;
  onEdit: (tx: LedgerTransaction) => void;
  onReceiptPdf: (tx: LedgerTransaction) => void;
}) {
  if (rows.length === 0) {
    return (
      <div className="bg-white border border-[#e2ddd2] rounded-xl p-8 text-sm text-zinc-500">
        {strings.noTransactionsYet}
      </div>
    );
  }

  return (
    <div className="bg-white border border-[#e2ddd2] rounded-xl overflow-hidden divide-y divide-[#e2ddd2]">
      {rows.map((tx) => {
        const isIncome = tx.type === TransactionType.INCOME_CUSTOMER_PAYMENT;
        const projName = projectMap.get(tx.projectId)?.name;
        const qtyStr =
          tx.quantity > 0 ? `${formatQty(tx.quantity)} ${tx.unit}` : undefined;
        const meta = [
          categoryLabel(tx.category, strings),
          qtyStr,
          tx.partyName,
          projName,
          formatDate(tx.dateMillis),
          paymentModeLabel(tx.paymentMode, strings),
          tx.referenceNo,
        ]
          .filter(Boolean)
          .join(' · ');

        return (
          <div
            key={tx.id}
            onClick={() => onEdit(tx)}
            className="px-4 py-3.5 flex items-center justify-between gap-4 hover:bg-[#f7f5f0] cursor-pointer transition-colors"
          >
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-9 h-9 rounded-lg bg-[#efece4] flex items-center justify-center shrink-0">
                {isIncome ? (
                  <ArrowDownLeft className="w-4 h-4 text-[#0f766e]" />
                ) : (
                  <ArrowUpRight className="w-4 h-4 text-[#b91c1c]" />
                )}
              </div>
              <div className="min-w-0">
                <p className="text-sm font-medium text-zinc-900 truncate">{tx.title}</p>
                <p className="text-xs text-zinc-500 truncate mt-0.5">{meta}</p>
              </div>
            </div>

            <div className="flex items-center gap-2 shrink-0">
              <span
                className={`text-sm font-semibold font-mono-num ${
                  isIncome ? 'text-[#0f766e]' : 'text-[#b91c1c]'
                }`}
              >
                {isIncome ? '+ ' : '- '}
                {formatInr(tx.amount)}
              </span>
              {isIncome && (
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    onReceiptPdf(tx);
                  }}
                  title={strings.generateReceiptPdf}
                  className="p-1.5 text-[#9a3412] hover:bg-[#efece4] rounded-md"
                >
                  <FileDown className="w-4 h-4" />
                </button>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}

function DailySiteGroupedList({
  rows,
  projectMap,
  strings,
  onEdit,
}: {
  rows: LedgerTransaction[];
  projectMap: Map<string, Project>;
  strings: ReturnType<typeof getStrings>;
  onEdit: (tx: LedgerTransaction) => void;
}) {
  if (rows.length === 0) {
    return (
      <div className="bg-white border border-[#e2ddd2] rounded-xl p-8 text-sm text-zinc-500">
        {strings.noTransactionsYet}
      </div>
    );
  }

  const groups = new Map<string, LedgerTransaction[]>();
  for (const tx of rows) {
    const label = isToday(tx.dateMillis)
      ? strings.today
      : isYesterday(tx.dateMillis)
        ? strings.yesterday
        : formatDate(tx.dateMillis);
    const list = groups.get(label) ?? [];
    list.push(tx);
    groups.set(label, list);
  }

  return (
    <div className="space-y-4">
      {Array.from(groups.entries()).map(([label, entries]) => {
        const dayTotal = entries.reduce((sum, e) => sum + e.amount, 0);
        return (
          <div key={label} className="space-y-2">
            <div className="flex items-center justify-between text-xs font-semibold">
              <span className="text-zinc-600">{label}</span>
              <span className="font-mono-num text-[#b91c1c]">{formatInr(dayTotal)}</span>
            </div>
            <TransactionTable
              rows={entries}
              projectMap={projectMap}
              strings={strings}
              onEdit={onEdit}
              onReceiptPdf={() => {}}
            />
          </div>
        );
      })}
    </div>
  );
}
