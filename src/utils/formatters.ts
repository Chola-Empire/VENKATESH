const inrFormatter = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
});

const inrPreciseFormatter = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function formatInr(amount: number, showDecimals = false): string {
  const fmt = showDecimals ? inrPreciseFormatter : inrFormatter;
  const absFormatted = fmt.format(Math.abs(amount));
  return amount < 0 ? `-${absFormatted}` : absFormatted;
}

export function formatQty(qty: number): string {
  return Number.isInteger(qty) ? String(qty) : qty.toFixed(2);
}

export function formatDate(millis: number): string {
  return new Intl.DateTimeFormat('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date(millis));
}

export function formatInputDate(millis: number): string {
  const d = new Date(millis);
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

export function formatFileStamp(millis = Date.now()): string {
  const d = new Date(millis);
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  const hh = String(d.getHours()).padStart(2, '0');
  const min = String(d.getMinutes()).padStart(2, '0');
  return `${yyyy}${mm}${dd}_${hh}${min}`;
}

export function isToday(millis: number): boolean {
  const a = new Date(millis);
  const b = new Date();
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

export function isYesterday(millis: number): boolean {
  const a = new Date(millis);
  const b = new Date();
  b.setDate(b.getDate() - 1);
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

export function startOfMonthMillis(): number {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), 1).getTime();
}
