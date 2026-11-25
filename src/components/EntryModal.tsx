import React, { useState } from 'react';
import { FileDown, Trash2, X } from 'lucide-react';
import { AppStrings, categoryLabel, paymentModeLabel, txTypeLabel } from '../i18n/strings';
import {
  AccountCategory,
  CATEGORY_BY_TYPE,
  Client,
  DEFAULT_UNIT_BY_CATEGORY,
  LedgerTransaction,
  PaymentMode,
  Project,
  SyncStatus,
  TransactionType,
} from '../types/accounts';
import { formatInputDate } from '../utils/formatters';

interface EntryModalProps {
  existing: LedgerTransaction | null;
  initialType: TransactionType;
  initialProjectId?: string;
  initialClientId?: string;
  projects: Project[];
  clients: Client[];
  strings: AppStrings;
  onClose: () => void;
  onSave: (tx: LedgerTransaction) => void;
  onDelete?: (id: string) => void;
  onReceiptPdf?: (tx: LedgerTransaction) => void;
}

export const EntryModal: React.FC<EntryModalProps> = ({
  existing,
  initialType,
  initialProjectId,
  initialClientId,
  projects,
  clients,
  strings,
  onClose,
  onSave,
  onDelete,
  onReceiptPdf,
}) => {
  const [type, setType] = useState<TransactionType>(existing?.type ?? initialType);
  const [category, setCategory] = useState<AccountCategory>(
    existing?.category ?? CATEGORY_BY_TYPE[initialType][0]
  );
  const [projectId, setProjectId] = useState<string>(
    existing?.projectId ?? initialProjectId ?? projects[0]?.id ?? ''
  );
  const [clientId, setClientId] = useState<string>(
    existing?.clientId ??
      initialClientId ??
      projects.find((p) => p.id === (existing?.projectId ?? initialProjectId ?? projects[0]?.id))
        ?.clientId ??
      ''
  );
  const [title, setTitle] = useState(existing?.title ?? '');
  const [partyName, setPartyName] = useState(() => {
    if (existing?.partyName) return existing.partyName;
    if ((existing?.type ?? initialType) === TransactionType.INCOME_CUSTOMER_PAYMENT) {
      const c = clients.find((item) => item.id === (initialClientId ?? ''));
      return c?.name ?? '';
    }
    return '';
  });
  const [quantity, setQuantity] = useState(existing?.quantity ? String(existing.quantity) : '');
  const [unit, setUnit] = useState(existing?.unit ?? DEFAULT_UNIT_BY_CATEGORY[category] ?? '');
  const [unitRate, setUnitRate] = useState(existing?.unitRate ? String(existing.unitRate) : '');
  const [amount, setAmount] = useState(existing?.amount ? String(existing.amount) : '');
  const [paymentMode, setPaymentMode] = useState<PaymentMode>(
    existing?.paymentMode ?? PaymentMode.CASH
  );
  const [referenceNo, setReferenceNo] = useState(existing?.referenceNo ?? '');
  const [dateStr, setDateStr] = useState(formatInputDate(existing?.dateMillis ?? Date.now()));
  const [notes, setNotes] = useState(existing?.notes ?? '');

  const handleTypeChange = (nextType: TransactionType) => {
    setType(nextType);
    const firstCat = CATEGORY_BY_TYPE[nextType][0];
    setCategory(firstCat);
    setUnit(DEFAULT_UNIT_BY_CATEGORY[firstCat] ?? '');
    if (nextType === TransactionType.INCOME_CUSTOMER_PAYMENT && !partyName) {
      const c = clients.find((item) => item.id === clientId);
      if (c) setPartyName(c.name);
    }
  };

  const recalc = (qStr: string, rStr: string) => {
    const q = parseFloat(qStr);
    const r = parseFloat(rStr);
    if (!Number.isNaN(q) && !Number.isNaN(r) && q > 0 && r > 0) {
      const total = q * r;
      setAmount(Number.isInteger(total) ? String(total) : total.toFixed(2));
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const parsedAmount = parseFloat(amount);
    if (!projectId || Number.isNaN(parsedAmount) || parsedAmount <= 0) return;

    const dateMillis = dateStr ? new Date(`${dateStr}T12:00:00`).getTime() : Date.now();
    onSave({
      id: existing?.id ?? `tx_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
      projectId,
      clientId: type === TransactionType.INCOME_CUSTOMER_PAYMENT ? clientId : '',
      type,
      category,
      title: title.trim() || categoryLabel(category, strings),
      partyName: partyName.trim(),
      quantity: parseFloat(quantity) || 0,
      unit: unit.trim(),
      unitRate: parseFloat(unitRate) || 0,
      amount: parsedAmount,
      paymentMode,
      referenceNo: referenceNo.trim(),
      dateMillis: Number.isNaN(dateMillis) ? Date.now() : dateMillis,
      notes: notes.trim(),
      createdAt: existing?.createdAt ?? Date.now(),
      syncStatus: SyncStatus.LOCAL_ONLY,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
      <div className="bg-white border border-[#e2ddd2] rounded-xl w-full max-w-xl max-h-[90vh] overflow-y-auto p-6">
        <div className="flex items-center justify-between pb-4 border-b border-[#e2ddd2]">
          <h3 className="text-lg font-semibold text-zinc-900">
            {existing ? strings.edit : strings.quickEntry}
          </h3>
          <div className="flex items-center gap-2">
            {existing &&
              existing.type === TransactionType.INCOME_CUSTOMER_PAYMENT &&
              onReceiptPdf && (
                <button
                  type="button"
                  onClick={() => onReceiptPdf(existing)}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-[#9a3412] border border-[#e2ddd2] rounded-lg hover:bg-[#f7f5f0] whitespace-nowrap shrink-0"
                >
                  <FileDown className="w-3.5 h-3.5" />
                  {strings.generateReceiptPdf}
                </button>
              )}
            {existing && onDelete && (
              <button
                type="button"
                onClick={() => {
                  onDelete(existing.id);
                  onClose();
                }}
                className="p-2 text-red-700 hover:bg-red-50 rounded-lg"
                title={strings.delete}
              >
                <Trash2 className="w-4 h-4" />
              </button>
            )}
            <button
              type="button"
              onClick={onClose}
              className="p-2 text-zinc-500 hover:bg-zinc-100 rounded-lg"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            {Object.values(TransactionType).map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => handleTypeChange(t)}
                className={`px-3 py-2 text-xs font-medium rounded-lg border transition-colors whitespace-nowrap truncate ${
                  type === t
                    ? 'bg-[#9a3412] text-white border-[#9a3412]'
                    : 'bg-white text-zinc-700 border-[#e2ddd2] hover:bg-[#f7f5f0]'
                }`}
              >
                {txTypeLabel(t, strings)}
              </button>
            ))}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-zinc-600 mb-1">
                {strings.selectProject}
              </label>
              <select
                value={projectId}
                onChange={(e) => {
                  setProjectId(e.target.value);
                  const proj = projects.find((p) => p.id === e.target.value);
                  if (proj?.clientId && type === TransactionType.INCOME_CUSTOMER_PAYMENT) {
                    setClientId(proj.clientId);
                    const c = clients.find((item) => item.id === proj.clientId);
                    if (c && !partyName) setPartyName(c.name);
                  }
                }}
                className="w-full px-3 py-2 text-sm bg-white border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
              >
                {projects.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-zinc-600 mb-1">
                {strings.selectCategory}
              </label>
              <select
                value={category}
                onChange={(e) => {
                  const cat = e.target.value as AccountCategory;
                  setCategory(cat);
                  setUnit(DEFAULT_UNIT_BY_CATEGORY[cat] ?? '');
                }}
                className="w-full px-3 py-2 text-sm bg-white border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
              >
                {CATEGORY_BY_TYPE[type].map((cat) => (
                  <option key={cat} value={cat}>
                    {categoryLabel(cat, strings)}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {type === TransactionType.INCOME_CUSTOMER_PAYMENT && (
            <div>
              <label className="block text-xs font-medium text-zinc-600 mb-1">
                {strings.selectClient}
              </label>
              <select
                value={clientId}
                onChange={(e) => {
                  setClientId(e.target.value);
                  const c = clients.find((item) => item.id === e.target.value);
                  if (c) setPartyName(c.name);
                }}
                className="w-full px-3 py-2 text-sm bg-white border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
              >
                <option value="">—</option>
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-zinc-600 mb-1">
                {strings.titleDescLabel}
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder={categoryLabel(category, strings)}
                className="w-full px-3 py-2 text-sm border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-zinc-600 mb-1">
                {strings.partyNameLabel}
              </label>
              <input
                type="text"
                value={partyName}
                onChange={(e) => setPartyName(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
              />
            </div>
          </div>

          {type !== TransactionType.INCOME_CUSTOMER_PAYMENT && (
            <div className="grid grid-cols-3 gap-3">
              <div>
                <label className="block text-xs font-medium text-zinc-600 mb-1">
                  {strings.quantity}
                </label>
                <input
                  type="number"
                  step="any"
                  min="0"
                  value={quantity}
                  onChange={(e) => {
                    setQuantity(e.target.value);
                    recalc(e.target.value, unitRate);
                  }}
                  className="w-full px-3 py-2 text-sm font-mono-num border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-zinc-600 mb-1">
                  {strings.unit}
                </label>
                <input
                  type="text"
                  value={unit}
                  onChange={(e) => setUnit(e.target.value)}
                  placeholder="Bags / Kg / Units"
                  className="w-full px-3 py-2 text-sm border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-zinc-600 mb-1">
                  {strings.unitRate}
                </label>
                <input
                  type="number"
                  step="any"
                  min="0"
                  value={unitRate}
                  onChange={(e) => {
                    setUnitRate(e.target.value);
                    recalc(quantity, e.target.value);
                  }}
                  className="w-full px-3 py-2 text-sm font-mono-num border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
                />
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-medium text-zinc-600 mb-1">
                {strings.amountInr} *
              </label>
              <input
                type="number"
                step="any"
                min="1"
                required
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="w-full px-3 py-2 text-sm font-mono-num font-semibold border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-zinc-600 mb-1">
                {strings.paymentMode}
              </label>
              <select
                value={paymentMode}
                onChange={(e) => setPaymentMode(e.target.value as PaymentMode)}
                className="w-full px-3 py-2 text-sm bg-white border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
              >
                {Object.values(PaymentMode).map((m) => (
                  <option key={m} value={m}>
                    {paymentModeLabel(m, strings)}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-zinc-600 mb-1">
                {strings.dateLabel}
              </label>
              <input
                type="date"
                value={dateStr}
                onChange={(e) => setDateStr(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-zinc-600 mb-1">
                {strings.referenceNo}
              </label>
              <input
                type="text"
                value={referenceNo}
                onChange={(e) => setReferenceNo(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-zinc-600 mb-1">
                {strings.notes}
              </label>
              <input
                type="text"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
              />
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-[#e2ddd2]">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-zinc-700 border border-[#e2ddd2] rounded-lg hover:bg-[#f7f5f0]"
            >
              {strings.cancel}
            </button>
            <button
              type="submit"
              disabled={!projectId || !amount || parseFloat(amount) <= 0}
              className="px-5 py-2 text-sm font-medium bg-[#9a3412] text-white rounded-lg hover:bg-[#7c2d12] disabled:opacity-50"
            >
              {strings.save}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
