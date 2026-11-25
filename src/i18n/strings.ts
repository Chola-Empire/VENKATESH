import {
  AccountCategory,
  PaymentMode,
  ProjectStatus,
  TransactionType,
} from '../types/accounts';

export type AppLanguage = 'en' | 'ta';

export interface AppStrings {
  brandTitle: string;
  brandSubtitle: string;
  navOverview: string;
  navProjects: string;
  navAccounts: string;
  navClients: string;
  navReports: string;
  quickEntry: string;
  offlineStatus: string;
  onlineStatus: string;
  totalContract: string;
  totalReceived: string;
  totalExpenses: string;
  netProfitLoss: string;
  pendingBalance: string;
  quickActions: string;
  addCustomerPayment: string;
  addMaterial: string;
  addLabour: string;
  addSiteExpense: string;
  activeProjects: string;
  recentTransactions: string;
  noProjectsYet: string;
  noTransactionsYet: string;
  noClientsYet: string;
  addProject: string;
  editProject: string;
  addClient: string;
  editClient: string;
  projectName: string;
  clientName: string;
  location: string;
  contractValue: string;
  status: string;
  statusActive: string;
  statusOnHold: string;
  statusCompleted: string;
  phone: string;
  address: string;
  gstNumber: string;
  notes: string;
  save: string;
  cancel: string;
  delete: string;
  edit: string;
  close: string;
  confirm: string;
  tabAllLedger: string;
  tabMaterials: string;
  tabLabour: string;
  tabDailySite: string;
  filterAllProjects: string;
  searchPlaceholder: string;
  cement: string;
  steel: string;
  sand: string;
  bricksAggregate: string;
  otherMaterial: string;
  mason: string;
  helper: string;
  contractor: string;
  electricianPlumber: string;
  transport: string;
  foodTea: string;
  equipmentRent: string;
  utilityPetty: string;
  customerAdvance: string;
  milestonePayment: string;
  finalSettlement: string;
  quantity: string;
  unit: string;
  unitRate: string;
  amountInr: string;
  partyNameLabel: string;
  titleDescLabel: string;
  paymentMode: string;
  modeCash: string;
  modeUpi: string;
  modeBank: string;
  modeCheque: string;
  referenceNo: string;
  dateLabel: string;
  selectProject: string;
  selectClient: string;
  selectCategory: string;
  entryType: string;
  typeIncome: string;
  typeMaterial: string;
  typeLabour: string;
  typeSiteExpense: string;
  income: string;
  expense: string;
  profit: string;
  loss: string;
  profitMargin: string;
  profitLossReport: string;
  dateRangeAll: string;
  dateRangeMonth: string;
  dateRange30Days: string;
  expenseBreakdown: string;
  generateInvoicePdf: string;
  generateReceiptPdf: string;
  exportReportPdf: string;
  printStatement: string;
  backupAndRestore: string;
  cloudBackupTitle: string;
  cloudBackupDesc: string;
  signInGoogleSync: string;
  signOutGoogle: string;
  syncNow: string;
  syncing: string;
  signedInAs: string;
  localQueueLabel: string;
  allSyncedLabel: string;
  exportJsonBackup: string;
  restoreJsonBackup: string;
  exportCsvLedger: string;
  clearAllData: string;
  confirmClearMessage: string;
  confirmRestoreMessage: string;
  backupExportedSuccess: string;
  csvExportedSuccess: string;
  restoreSuccess: string;
  restoreFailed: string;
  syncSuccess: string;
  syncFailed: string;
  clientLedger: string;
  projectSummary: string;
  recordPayment: string;
  receivedFromClient: string;
  billedContract: string;
  bagsUnit: string;
  kgUnit: string;
  unitsLoad: string;
  today: string;
  yesterday: string;
  paymentReceipt: string;
  projectInvoice: string;
}

const en: AppStrings = {
  brandTitle: 'Chola Empire',
  brandSubtitle: 'Construction Accounts',
  navOverview: 'Overview',
  navProjects: 'Projects',
  navAccounts: 'Accounts',
  navClients: 'Clients',
  navReports: 'Reports & Backup',
  quickEntry: 'New Entry',
  offlineStatus: 'Offline Mode · Local Entry Active',
  onlineStatus: 'Online · Auto-Sync Ready',
  totalContract: 'Contract Value',
  totalReceived: 'Received',
  totalExpenses: 'Expenses',
  netProfitLoss: 'Net Profit / Loss',
  pendingBalance: 'Pending Balance',
  quickActions: 'Quick Entry',
  addCustomerPayment: 'Customer Payment',
  addMaterial: 'Material Account',
  addLabour: 'Labour & Contractor',
  addSiteExpense: 'Daily Site Expense',
  activeProjects: 'Active Projects',
  recentTransactions: 'Recent Ledger Entries',
  noProjectsYet: 'No projects yet. Create a project to track income and expenses.',
  noTransactionsYet: 'No entries found for the current filter.',
  noClientsYet: 'No clients yet. Add a client to track pending balances.',
  addProject: 'Add Project',
  editProject: 'Edit Project',
  addClient: 'Add Client',
  editClient: 'Edit Client',
  projectName: 'Project Name',
  clientName: 'Client Name',
  location: 'Site Location',
  contractValue: 'Contract Value (₹)',
  status: 'Status',
  statusActive: 'Active',
  statusOnHold: 'On Hold',
  statusCompleted: 'Completed',
  phone: 'Phone',
  address: 'Address',
  gstNumber: 'GSTIN (Optional)',
  notes: 'Notes',
  save: 'Save',
  cancel: 'Cancel',
  delete: 'Delete',
  edit: 'Edit',
  close: 'Close',
  confirm: 'Confirm',
  tabAllLedger: 'All Ledger',
  tabMaterials: 'Materials',
  tabLabour: 'Labour & Contractor',
  tabDailySite: 'Daily Site',
  filterAllProjects: 'All Projects',
  searchPlaceholder: 'Search item, party, bill no...',
  cement: 'Cement',
  steel: 'Steel / TMT',
  sand: 'Sand / M-Sand',
  bricksAggregate: 'Bricks & Jelly',
  otherMaterial: 'Other Materials',
  mason: 'Mason (Kothanar)',
  helper: 'Helper (Chithal)',
  contractor: 'Sub-Contractor',
  electricianPlumber: 'Electrical & Plumbing',
  transport: 'Transport / Freight',
  foodTea: 'Site Food & Tea',
  equipmentRent: 'Equipment / JCB Rent',
  utilityPetty: 'Site Utility & Petty',
  customerAdvance: 'Advance Payment',
  milestonePayment: 'Stage Payment',
  finalSettlement: 'Final Settlement',
  quantity: 'Quantity',
  unit: 'Unit',
  unitRate: 'Rate (₹)',
  amountInr: 'Amount (₹)',
  partyNameLabel: 'Supplier / Worker / Customer',
  titleDescLabel: 'Particulars / Description',
  paymentMode: 'Payment Mode',
  modeCash: 'Cash',
  modeUpi: 'UPI',
  modeBank: 'Bank / NEFT',
  modeCheque: 'Cheque',
  referenceNo: 'Bill / Receipt No.',
  dateLabel: 'Date',
  selectProject: 'Project',
  selectClient: 'Client',
  selectCategory: 'Category',
  entryType: 'Entry Type',
  typeIncome: 'Customer Payment',
  typeMaterial: 'Material Account',
  typeLabour: 'Labour & Contractor',
  typeSiteExpense: 'Daily Site Expense',
  income: 'Income',
  expense: 'Expense',
  profit: 'Profit',
  loss: 'Loss',
  profitMargin: 'Net Margin',
  profitLossReport: 'Profit & Loss Report',
  dateRangeAll: 'All Time',
  dateRangeMonth: 'This Month',
  dateRange30Days: 'Last 30 Days',
  expenseBreakdown: 'Cost Breakdown',
  generateInvoicePdf: 'Invoice PDF',
  generateReceiptPdf: 'Receipt PDF',
  exportReportPdf: 'Export P&L PDF',
  printStatement: 'Print',
  backupAndRestore: 'Data Export & Restore',
  cloudBackupTitle: 'Online Cloud Backup & Offline Entry',
  cloudBackupDesc:
    'Entries save immediately to your browser offline storage so site work continues without internet. Sign in with Google to back up and sync across devices via Cloud Firestore.',
  signInGoogleSync: 'Sign in with Google for Cloud Backup',
  signOutGoogle: 'Sign Out',
  syncNow: 'Sync Cloud Backup Now',
  syncing: 'Syncing...',
  signedInAs: 'Cloud Backup Linked',
  localQueueLabel: 'Pending local sync',
  allSyncedLabel: 'All entries backed up',
  exportJsonBackup: 'Export Backup (.json)',
  restoreJsonBackup: 'Restore Backup (.json)',
  exportCsvLedger: 'Export Ledger (.csv)',
  clearAllData: 'Reset All Records',
  confirmClearMessage: 'Delete all local projects, clients, and ledger entries? Export a JSON backup first.',
  confirmRestoreMessage: 'Replace current records with the selected backup file?',
  backupExportedSuccess: 'JSON backup downloaded.',
  csvExportedSuccess: 'CSV ledger downloaded.',
  restoreSuccess: 'Backup restored successfully.',
  restoreFailed: 'Could not parse backup JSON file.',
  syncSuccess: 'Cloud backup synced to Firestore.',
  syncFailed: 'Cloud sync paused (offline or sign-in required).',
  clientLedger: 'Client-wise Ledger',
  projectSummary: 'Project Accounts',
  recordPayment: 'Record Payment',
  receivedFromClient: 'Paid',
  billedContract: 'Contract',
  bagsUnit: 'Bags',
  kgUnit: 'Kg',
  unitsLoad: 'Units',
  today: 'Today',
  yesterday: 'Yesterday',
  paymentReceipt: 'PAYMENT RECEIPT',
  projectInvoice: 'PROJECT INVOICE & STATEMENT',
};

const ta: AppStrings = {
  brandTitle: 'சோழா எம்பயர்',
  brandSubtitle: 'கட்டுமான கணக்குகள்',
  navOverview: 'முகப்பு',
  navProjects: 'திட்டங்கள்',
  navAccounts: 'கணக்குகள்',
  navClients: 'வாடிக்கையாளர்',
  navReports: 'அறிக்கை & காப்பு',
  quickEntry: 'புதிய பதிவு',
  offlineStatus: 'ஆஃப்லைன் நிலை · சாதனத்தில் சேமிப்பு',
  onlineStatus: 'ஆன்லைன் · கிளவுட் ஒத்திசைவு தயார்',
  totalContract: 'ஒப்பந்த மதிப்பு',
  totalReceived: 'பெற்ற தொகை',
  totalExpenses: 'மொத்த செலவு',
  netProfitLoss: 'நிகர லாபம் / நஷ்டம்',
  pendingBalance: 'நிலுவைத் தொகை',
  quickActions: 'விரைவுப் பதிவு',
  addCustomerPayment: 'வாடிக்கையாளர் வரவு',
  addMaterial: 'பொருள் கணக்கு',
  addLabour: 'தொழிலாளர் / ஒப்பந்ததாரர்',
  addSiteExpense: 'தினசரி தளச் செலவு',
  activeProjects: 'நடப்பு திட்டங்கள்',
  recentTransactions: 'சமீபத்திய கணக்குப் பதிவுகள்',
  noProjectsYet: 'திட்டங்கள் இல்லை. புதிய திட்டத்தை உருவாக்கவும்.',
  noTransactionsYet: 'இந்தப் பிரிவில் பதிவுகள் எதுவும் இல்லை.',
  noClientsYet: 'வாடிக்கையாளர்கள் இல்லை. புதிய வாடிக்கையாளரைச் சேர்க்கவும்.',
  addProject: 'திட்டம் சேர்',
  editProject: 'திட்டம் திருத்து',
  addClient: 'வாடிக்கையாளர் சேர்',
  editClient: 'வாடிக்கையாளர் திருத்து',
  projectName: 'திட்டத்தின் பெயர்',
  clientName: 'வாடிக்கையாளர் பெயர்',
  location: 'தள இடம்',
  contractValue: 'ஒப்பந்தத் தொகை (₹)',
  status: 'நிலை',
  statusActive: 'நடைபெறுகிறது',
  statusOnHold: 'நிறுத்திவைப்பு',
  statusCompleted: 'முடிந்தது',
  phone: 'தொலைபேசி',
  address: 'முகவரி',
  gstNumber: 'GST எண் (விருப்பம்)',
  notes: 'குறிப்புகள்',
  save: 'சேமி',
  cancel: 'ரத்து',
  delete: 'நீக்கு',
  edit: 'திருத்து',
  close: 'மூடு',
  confirm: 'உறுதி செய்',
  tabAllLedger: 'முழுப் பேரேடு',
  tabMaterials: 'பொருட்கள்',
  tabLabour: 'தொழிலாளர்',
  tabDailySite: 'தினசரி செலவு',
  filterAllProjects: 'அனைத்து திட்டங்கள்',
  searchPlaceholder: 'பெயர், பொருள், பில் எண் தேடு...',
  cement: 'சிமெண்ட்',
  steel: 'கம்பி (ஸ்டீல்)',
  sand: 'மணல் / எம்-சாண்ட்',
  bricksAggregate: 'செங்கல் & ஜல்லி',
  otherMaterial: 'பிற பொருட்கள்',
  mason: 'கொத்தனார்',
  helper: 'சித்தாள் / உதவியாளர்',
  contractor: 'துணை ஒப்பந்ததாரர்',
  electricianPlumber: 'எலக்ட்ரீஷியன் / பிளம்பர்',
  transport: 'வாகன வாடகை',
  foodTea: 'தள உணவு & தேநீர்',
  equipmentRent: 'இயந்திர / JCB வாடகை',
  utilityPetty: 'தள இதர செலவு',
  customerAdvance: 'முன்பணம்',
  milestonePayment: 'நிலை வாரியான தொகை',
  finalSettlement: 'இறுதித் தொகை',
  quantity: 'அளவு',
  unit: 'அலகு',
  unitRate: 'விலை (₹)',
  amountInr: 'தொகை (₹)',
  partyNameLabel: 'விற்பனையாளர் / தொழிலாளர் / வாடிக்கையாளர்',
  titleDescLabel: 'வேலை / பொருள் விவரம்',
  paymentMode: 'செலுத்தும் முறை',
  modeCash: 'ரொக்கம்',
  modeUpi: 'UPI',
  modeBank: 'வங்கி / NEFT',
  modeCheque: 'காசோலை',
  referenceNo: 'பில் / ரசீது எண்',
  dateLabel: 'தேதி',
  selectProject: 'திட்டம்',
  selectClient: 'வாடிக்கையாளர்',
  selectCategory: 'பிரிவு',
  entryType: 'பதிவு வகை',
  typeIncome: 'வாடிக்கையாளர் வரவு',
  typeMaterial: 'பொருள் கணக்கு',
  typeLabour: 'தொழிலாளர் சம்பளம்',
  typeSiteExpense: 'தினசரி தளச் செலவு',
  income: 'வரவு',
  expense: 'செலவு',
  profit: 'லாபம்',
  loss: 'நஷ்டம்',
  profitMargin: 'லாப விகிதம்',
  profitLossReport: 'லாப நஷ்ட அறிக்கை',
  dateRangeAll: 'முழு காலம்',
  dateRangeMonth: 'இந்த மாதம்',
  dateRange30Days: 'கடந்த 30 நாட்கள்',
  expenseBreakdown: 'செலவுப் பிரிவுகள்',
  generateInvoicePdf: 'பில் (Invoice PDF)',
  generateReceiptPdf: 'ரசீது (Receipt PDF)',
  exportReportPdf: 'லாப நஷ்ட PDF',
  printStatement: 'அச்சிடு',
  backupAndRestore: 'தரவு காப்பு & மீட்பு',
  cloudBackupTitle: 'ஆன்லைன் கிளவுட் காப்பு & ஆஃப்லைன் பதிவு',
  cloudBackupDesc:
    'இணையம் இல்லாவிட்டாலும் அனைத்து பதிவுகளும் ஆஃப்லைனில் உடனடியாக சேமிக்கப்படும். Google உள்நுழைவு மூலம் கிளவுட் காப்பை ஒத்திசைக்கலாம்.',
  signInGoogleSync: 'கிளவுட் காப்புக்கு Google உள்நுழைவு',
  signOutGoogle: 'வெளியேறு',
  syncNow: 'கிளவுட் காப்பை ஒத்திசை',
  syncing: 'ஒத்திசைக்கிறது...',
  signedInAs: 'கிளவுட் கணக்கு இணைக்கப்பட்டது',
  localQueueLabel: 'ஒத்திசைக்க வேண்டிய பதிவுகள்',
  allSyncedLabel: 'அனைத்து பதிவுகளும் காப்பு செய்யப்பட்டது',
  exportJsonBackup: 'காப்புப் பதிவிறக்கம் (.json)',
  restoreJsonBackup: 'காப்பிலிருந்து மீட்டெடு (.json)',
  exportCsvLedger: 'Excel / CSV ஏற்றுமதி',
  clearAllData: 'அனைத்தையும் அழி',
  confirmClearMessage: 'அனைத்து திட்டங்கள், வாடிக்கையாளர்கள் மற்றும் பதிவுகளை அழிக்கவா?',
  confirmRestoreMessage: 'தேர்ந்தெடுக்கப்பட்ட காப்புக் கோப்பை மீட்டெடுக்கவா?',
  backupExportedSuccess: 'JSON காப்புக் கோப்பு பதிவிறக்கப்பட்டது.',
  csvExportedSuccess: 'CSV பேரேடு பதிவிறக்கப்பட்டது.',
  restoreSuccess: 'தரவுகள் வெற்றிகரமாக மீட்டெடுக்கப்பட்டன.',
  restoreFailed: 'காப்புக் கோப்பைப் படிக்க முடியவில்லை.',
  syncSuccess: 'கிளவுட் காப்பு வெற்றிகரமாக ஒத்திசைக்கப்பட்டது.',
  syncFailed: 'ஒத்திசைவு தற்காலிகமாக நிறுத்தப்பட்டுள்ளது.',
  clientLedger: 'வாடிக்கையாளர் பேரேடு',
  projectSummary: 'திட்டக் கணக்குகள்',
  recordPayment: 'வரவு பதிவு செய்',
  receivedFromClient: 'பெற்றது',
  billedContract: 'ஒப்பந்தம்',
  bagsUnit: 'மூட்டை',
  kgUnit: 'கிலோ',
  unitsLoad: 'யூனிட்',
  today: 'இன்று',
  yesterday: 'நேற்று',
  paymentReceipt: 'பணம் பெற்ற ரசீது',
  projectInvoice: 'திட்ட பில் & கணக்கு அறிக்கை',
};

export function getStrings(lang: AppLanguage): AppStrings {
  return lang === 'ta' ? ta : en;
}

export function categoryLabel(category: AccountCategory, s: AppStrings): string {
  const map: Record<AccountCategory, string> = {
    [AccountCategory.CUSTOMER_ADVANCE]: s.customerAdvance,
    [AccountCategory.MILESTONE_PAYMENT]: s.milestonePayment,
    [AccountCategory.FINAL_SETTLEMENT]: s.finalSettlement,
    [AccountCategory.CEMENT]: s.cement,
    [AccountCategory.STEEL]: s.steel,
    [AccountCategory.SAND]: s.sand,
    [AccountCategory.BRICKS_AGGREGATE]: s.bricksAggregate,
    [AccountCategory.OTHER_MATERIAL]: s.otherMaterial,
    [AccountCategory.MASON]: s.mason,
    [AccountCategory.HELPER]: s.helper,
    [AccountCategory.CONTRACTOR]: s.contractor,
    [AccountCategory.ELECTRICIAN_PLUMBER]: s.electricianPlumber,
    [AccountCategory.TRANSPORT]: s.transport,
    [AccountCategory.FOOD_TEA]: s.foodTea,
    [AccountCategory.EQUIPMENT_RENT]: s.equipmentRent,
    [AccountCategory.UTILITY_PETTY]: s.utilityPetty,
  };
  return map[category];
}

export function paymentModeLabel(mode: PaymentMode, s: AppStrings): string {
  const map: Record<PaymentMode, string> = {
    [PaymentMode.CASH]: s.modeCash,
    [PaymentMode.UPI]: s.modeUpi,
    [PaymentMode.BANK_TRANSFER]: s.modeBank,
    [PaymentMode.CHEQUE]: s.modeCheque,
  };
  return map[mode];
}

export function projectStatusLabel(status: ProjectStatus, s: AppStrings): string {
  const map: Record<ProjectStatus, string> = {
    [ProjectStatus.ACTIVE]: s.statusActive,
    [ProjectStatus.ON_HOLD]: s.statusOnHold,
    [ProjectStatus.COMPLETED]: s.statusCompleted,
  };
  return map[status];
}

export function txTypeLabel(type: TransactionType, s: AppStrings): string {
  const map: Record<TransactionType, string> = {
    [TransactionType.INCOME_CUSTOMER_PAYMENT]: s.typeIncome,
    [TransactionType.EXPENSE_MATERIAL]: s.typeMaterial,
    [TransactionType.EXPENSE_LABOUR]: s.typeLabour,
    [TransactionType.EXPENSE_SITE]: s.typeSiteExpense,
  };
  return map[type];
}
