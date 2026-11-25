import {
  collection,
  doc,
  getDocs,
  onSnapshot,
  serverTimestamp,
  writeBatch,
  type Unsubscribe,
} from 'firebase/firestore';
import { db, handleFirestoreError, OperationType } from '../firebase';
import {
  AccountCategory,
  Client,
  LedgerTransaction,
  PaymentMode,
  Project,
  ProjectStatus,
  SyncStatus,
  TransactionType,
} from '../types/accounts';

export async function syncLocalDataToCloud(
  userId: string,
  clients: Client[],
  projects: Project[],
  transactions: LedgerTransaction[]
): Promise<void> {
  const workspaceRef = doc(db, 'users', userId);
  const batch = writeBatch(db);

  batch.set(
    workspaceRef,
    {
      uid: userId,
      companyName: 'Chola Empire Construction',
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    },
    { merge: true }
  );

  for (const c of clients) {
    const ref = doc(db, 'users', userId, 'clients', c.id);
    batch.set(ref, {
      id: c.id.slice(0, 128),
      ownerId: userId,
      name: (c.name || 'Client').slice(0, 120),
      phone: (c.phone || '').slice(0, 32),
      address: (c.address || '').slice(0, 240),
      gstNumber: (c.gstNumber || '').slice(0, 32),
      notes: (c.notes || '').slice(0, 500),
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  }

  for (const p of projects) {
    const ref = doc(db, 'users', userId, 'projects', p.id);
    batch.set(ref, {
      id: p.id.slice(0, 128),
      ownerId: userId,
      name: (p.name || 'Project').slice(0, 140),
      clientId: (p.clientId || '').slice(0, 128),
      location: (p.location || '').slice(0, 140),
      contractValue: Math.max(0, Number(p.contractValue) || 0),
      status: p.status,
      notes: (p.notes || '').slice(0, 500),
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  }

  for (const tx of transactions) {
    const ref = doc(db, 'users', userId, 'transactions', tx.id);
    batch.set(ref, {
      id: tx.id.slice(0, 128),
      ownerId: userId,
      projectId: tx.projectId.slice(0, 128),
      clientId: (tx.clientId || '').slice(0, 128),
      type: tx.type,
      category: tx.category.slice(0, 48),
      title: (tx.title || 'Entry').slice(0, 160),
      partyName: (tx.partyName || '').slice(0, 120),
      quantity: Math.max(0, Number(tx.quantity) || 0),
      unit: (tx.unit || '').slice(0, 32),
      unitRate: Math.max(0, Number(tx.unitRate) || 0),
      amount: Math.max(1, Number(tx.amount) || 1),
      paymentMode: tx.paymentMode,
      referenceNo: (tx.referenceNo || '').slice(0, 64),
      dateMillis: Number(tx.dateMillis) || Date.now(),
      notes: (tx.notes || '').slice(0, 500),
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  }

  try {
    await batch.commit();
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, `users/${userId}`);
  }
}

export async function fetchCloudBackupSnapshot(userId: string): Promise<{
  clients: Client[];
  projects: Project[];
  transactions: LedgerTransaction[];
}> {
  try {
    const [cSnap, pSnap, tSnap] = await Promise.all([
      getDocs(collection(db, 'users', userId, 'clients')),
      getDocs(collection(db, 'users', userId, 'projects')),
      getDocs(collection(db, 'users', userId, 'transactions')),
    ]);

    const clients: Client[] = cSnap.docs.map((d) => {
      const data = d.data();
      return {
        id: String(data.id ?? d.id),
        name: String(data.name ?? ''),
        phone: String(data.phone ?? ''),
        address: String(data.address ?? ''),
        gstNumber: String(data.gstNumber ?? ''),
        notes: String(data.notes ?? ''),
        createdAt: Date.now(),
        syncStatus: SyncStatus.SYNCED,
      };
    });

    const projects: Project[] = pSnap.docs.map((d) => {
      const data = d.data();
      return {
        id: String(data.id ?? d.id),
        name: String(data.name ?? ''),
        clientId: String(data.clientId ?? ''),
        location: String(data.location ?? ''),
        contractValue: Number(data.contractValue ?? 0),
        status: (data.status as ProjectStatus) ?? ProjectStatus.ACTIVE,
        notes: String(data.notes ?? ''),
        createdAt: Date.now(),
        syncStatus: SyncStatus.SYNCED,
      };
    });

    const transactions: LedgerTransaction[] = tSnap.docs.map((d) => {
      const data = d.data();
      return {
        id: String(data.id ?? d.id),
        projectId: String(data.projectId ?? ''),
        clientId: String(data.clientId ?? ''),
        type: (data.type as TransactionType) ?? TransactionType.EXPENSE_SITE,
        category: (data.category as AccountCategory) ?? AccountCategory.UTILITY_PETTY,
        title: String(data.title ?? ''),
        partyName: String(data.partyName ?? ''),
        quantity: Number(data.quantity ?? 0),
        unit: String(data.unit ?? ''),
        unitRate: Number(data.unitRate ?? 0),
        amount: Number(data.amount ?? 0),
        paymentMode: (data.paymentMode as PaymentMode) ?? PaymentMode.CASH,
        referenceNo: String(data.referenceNo ?? ''),
        dateMillis: Number(data.dateMillis ?? Date.now()),
        notes: String(data.notes ?? ''),
        createdAt: Date.now(),
        syncStatus: SyncStatus.SYNCED,
      };
    });

    return { clients, projects, transactions };
  } catch (error) {
    handleFirestoreError(error, OperationType.LIST, `users/${userId}`);
  }
}

export function subscribeWorkspaceCount(
  userId: string,
  onCount: (count: number) => void
): Unsubscribe {
  const path = `users/${userId}/transactions`;
  return onSnapshot(
    collection(db, path),
    (snap) => {
      onCount(snap.size);
    },
    (error) => {
      handleFirestoreError(error, OperationType.LIST, path);
    }
  );
}
