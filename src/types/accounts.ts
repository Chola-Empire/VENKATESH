export enum ProjectStatus {
  ACTIVE = 'ACTIVE',
  ON_HOLD = 'ON_HOLD',
  COMPLETED = 'COMPLETED',
}

export enum TransactionType {
  INCOME_CUSTOMER_PAYMENT = 'INCOME_CUSTOMER_PAYMENT',
  EXPENSE_MATERIAL = 'EXPENSE_MATERIAL',
  EXPENSE_LABOUR = 'EXPENSE_LABOUR',
  EXPENSE_SITE = 'EXPENSE_SITE',
}

export enum AccountCategory {
  CUSTOMER_ADVANCE = 'CUSTOMER_ADVANCE',
  MILESTONE_PAYMENT = 'MILESTONE_PAYMENT',
  FINAL_SETTLEMENT = 'FINAL_SETTLEMENT',
  CEMENT = 'CEMENT',
  STEEL = 'STEEL',
  SAND = 'SAND',
  BRICKS_AGGREGATE = 'BRICKS_AGGREGATE',
  OTHER_MATERIAL = 'OTHER_MATERIAL',
  MASON = 'MASON',
  HELPER = 'HELPER',
  CONTRACTOR = 'CONTRACTOR',
  ELECTRICIAN_PLUMBER = 'ELECTRICIAN_PLUMBER',
  TRANSPORT = 'TRANSPORT',
  FOOD_TEA = 'FOOD_TEA',
  EQUIPMENT_RENT = 'EQUIPMENT_RENT',
  UTILITY_PETTY = 'UTILITY_PETTY',
}

export enum PaymentMode {
  CASH = 'CASH',
  UPI = 'UPI',
  BANK_TRANSFER = 'BANK_TRANSFER',
  CHEQUE = 'CHEQUE',
}

export enum SyncStatus {
  LOCAL_ONLY = 'LOCAL_ONLY',
  SYNCED = 'SYNCED',
}

export interface Client {
  id: string;
  name: string;
  phone: string;
  address: string;
  gstNumber: string;
  notes: string;
  createdAt: number;
  syncStatus: SyncStatus;
}

export interface Project {
  id: string;
  name: string;
  clientId: string;
  location: string;
  contractValue: number;
  status: ProjectStatus;
  notes: string;
  createdAt: number;
  syncStatus: SyncStatus;
}

export interface LedgerTransaction {
  id: string;
  projectId: string;
  clientId: string;
  type: TransactionType;
  category: AccountCategory;
  title: string;
  partyName: string;
  quantity: number;
  unit: string;
  unitRate: number;
  amount: number;
  paymentMode: PaymentMode;
  referenceNo: string;
  dateMillis: number;
  notes: string;
  createdAt: number;
  syncStatus: SyncStatus;
}

export interface CholaBackupPayload {
  version: number;
  exportedAt: number;
  appName: string;
  clients: Client[];
  projects: Project[];
  transactions: LedgerTransaction[];
}

export const CATEGORY_BY_TYPE: Record<TransactionType, AccountCategory[]> = {
  [TransactionType.INCOME_CUSTOMER_PAYMENT]: [
    AccountCategory.CUSTOMER_ADVANCE,
    AccountCategory.MILESTONE_PAYMENT,
    AccountCategory.FINAL_SETTLEMENT,
  ],
  [TransactionType.EXPENSE_MATERIAL]: [
    AccountCategory.CEMENT,
    AccountCategory.STEEL,
    AccountCategory.SAND,
    AccountCategory.BRICKS_AGGREGATE,
    AccountCategory.OTHER_MATERIAL,
  ],
  [TransactionType.EXPENSE_LABOUR]: [
    AccountCategory.MASON,
    AccountCategory.HELPER,
    AccountCategory.CONTRACTOR,
    AccountCategory.ELECTRICIAN_PLUMBER,
  ],
  [TransactionType.EXPENSE_SITE]: [
    AccountCategory.TRANSPORT,
    AccountCategory.FOOD_TEA,
    AccountCategory.EQUIPMENT_RENT,
    AccountCategory.UTILITY_PETTY,
  ],
};

export const DEFAULT_UNIT_BY_CATEGORY: Partial<Record<AccountCategory, string>> = {
  [AccountCategory.CEMENT]: 'Bags',
  [AccountCategory.STEEL]: 'Kg',
  [AccountCategory.SAND]: 'Units',
  [AccountCategory.BRICKS_AGGREGATE]: 'Loads',
  [AccountCategory.OTHER_MATERIAL]: 'Units',
  [AccountCategory.MASON]: 'Days',
  [AccountCategory.HELPER]: 'Days',
  [AccountCategory.EQUIPMENT_RENT]: 'Hours',
};
