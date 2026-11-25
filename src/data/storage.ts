import { AppLanguage } from '../i18n/strings';
import { CholaBackupPayload, Client, LedgerTransaction, Project } from '../types/accounts';
import { createSeedData } from './seedData';

const STORAGE_KEY = 'chola_empire_accounts_store_v1';
const LANGUAGE_KEY = 'chola_empire_lang_v1';

export interface LocalStoreSnapshot {
  seeded: boolean;
  clients: Client[];
  projects: Project[];
  transactions: LedgerTransaction[];
}

export function loadLocalStore(): LocalStoreSnapshot {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<LocalStoreSnapshot>;
      return {
        seeded: Boolean(parsed.seeded),
        clients: Array.isArray(parsed.clients) ? parsed.clients : [],
        projects: Array.isArray(parsed.projects) ? parsed.projects : [],
        transactions: Array.isArray(parsed.transactions) ? parsed.transactions : [],
      };
    }
  } catch (err) {
    console.error('Failed to read local storage', err);
  }
  const seed = createSeedData();
  const initial: LocalStoreSnapshot = {
    seeded: true,
    clients: seed.clients,
    projects: seed.projects,
    transactions: seed.transactions,
  };
  saveLocalStore(initial);
  return initial;
}

export function saveLocalStore(snapshot: LocalStoreSnapshot): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot));
  } catch (err) {
    console.error('Failed to persist local storage', err);
  }
}

export function loadSavedLanguage(): AppLanguage {
  try {
    const val = localStorage.getItem(LANGUAGE_KEY);
    return val === 'ta' ? 'ta' : 'en';
  } catch {
    return 'en';
  }
}

export function saveLanguage(lang: AppLanguage): void {
  try {
    localStorage.setItem(LANGUAGE_KEY, lang);
  } catch {
    // ignore storage errors
  }
}

export function buildJsonBackup(snapshot: LocalStoreSnapshot): string {
  const payload: CholaBackupPayload = {
    version: 1,
    exportedAt: Date.now(),
    appName: 'Chola Empire Construction Accounts',
    clients: snapshot.clients,
    projects: snapshot.projects,
    transactions: snapshot.transactions,
  };
  return JSON.stringify(payload, null, 2);
}

export function parseJsonBackup(raw: string): CholaBackupPayload {
  const parsed = JSON.parse(raw) as Partial<CholaBackupPayload>;
  if (
    !parsed ||
    !Array.isArray(parsed.clients) ||
    !Array.isArray(parsed.projects) ||
    !Array.isArray(parsed.transactions)
  ) {
    throw new Error('Invalid backup structure');
  }
  return {
    version: typeof parsed.version === 'number' ? parsed.version : 1,
    exportedAt: typeof parsed.exportedAt === 'number' ? parsed.exportedAt : Date.now(),
    appName: typeof parsed.appName === 'string' ? parsed.appName : 'Chola Empire Construction Accounts',
    clients: parsed.clients,
    projects: parsed.projects,
    transactions: parsed.transactions,
  };
}

export function buildCsvLedger(
  transactions: LedgerTransaction[],
  projects: Project[],
  clients: Client[]
): string {
  const projectMap = new Map(projects.map((p) => [p.id, p.name]));
  const clientMap = new Map(clients.map((c) => [c.id, c.name]));

  const escapeCsv = (value: string) => `"${value.replace(/"/g, '""')}"`;

  const lines = [
    'Date,Project,Client,Type,Category,Title,Party,Quantity,Unit,UnitRate,AmountINR,PaymentMode,ReferenceNo,Notes',
  ];

  for (const tx of transactions) {
    const dateStr = new Date(tx.dateMillis).toISOString().slice(0, 10);
    lines.push(
      [
        dateStr,
        projectMap.get(tx.projectId) ?? '',
        tx.clientId ? (clientMap.get(tx.clientId) ?? '') : '',
        tx.type,
        tx.category,
        tx.title,
        tx.partyName,
        tx.quantity ? String(tx.quantity) : '',
        tx.unit ?? '',
        tx.unitRate ? String(tx.unitRate) : '',
        String(tx.amount),
        tx.paymentMode,
        tx.referenceNo,
        tx.notes,
      ]
        .map(escapeCsv)
        .join(',')
    );
  }

  return lines.join('\n');
}
