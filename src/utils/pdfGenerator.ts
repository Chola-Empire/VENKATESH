import { jsPDF } from 'jspdf';
import { AppStrings, categoryLabel, paymentModeLabel } from '../i18n/strings';
import { Client, LedgerTransaction, Project, TransactionType } from '../types/accounts';
import { formatDate, formatFileStamp, formatInr } from './formatters';

const A4_W = 1190; // 2x crisp canvas resolution for A4 (595pt x 2)
const A4_H = 1684;
const MARGIN = 80;

function createPageCanvas(): [HTMLCanvasElement, CanvasRenderingContext2D] {
  const canvas = document.createElement('canvas');
  canvas.width = A4_W;
  canvas.height = A4_H;
  const ctx = canvas.getContext('2d')!;
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(0, 0, A4_W, A4_H);
  return [canvas, ctx];
}

function drawBrandHeader(
  ctx: CanvasRenderingContext2D,
  docTitle: string,
  refNo: string
): number {
  ctx.fillStyle = '#9A3412';
  ctx.fillRect(0, 0, A4_W, 18);

  ctx.fillStyle = '#9A3412';
  ctx.font = 'bold 34px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
  ctx.fillText('CHOLA EMPIRE CONSTRUCTION', MARGIN, 92);

  ctx.fillStyle = '#52525B';
  ctx.font = '20px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
  ctx.fillText('Construction Accounts · Material & Site Engineering Ledger', MARGIN, 126);

  ctx.fillStyle = '#18181B';
  ctx.font = 'bold 26px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
  ctx.textAlign = 'right';
  ctx.fillText(docTitle, A4_W - MARGIN, 90);

  ctx.fillStyle = '#52525B';
  ctx.font = '20px "IBM Plex Mono", monospace';
  ctx.fillText(`Ref: ${refNo}`, A4_W - MARGIN, 124);
  ctx.textAlign = 'left';

  ctx.strokeStyle = '#E2DDD2';
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(MARGIN, 152);
  ctx.lineTo(A4_W - MARGIN, 152);
  ctx.stroke();

  return 184;
}

function drawFooter(ctx: CanvasRenderingContext2D) {
  ctx.strokeStyle = '#E2DDD2';
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(MARGIN, A4_H - 120);
  ctx.lineTo(A4_W - MARGIN, A4_H - 120);
  ctx.stroke();

  ctx.fillStyle = '#71717A';
  ctx.font = '18px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
  ctx.fillText('Chola Empire Construction Accounts', MARGIN, A4_H - 78);

  ctx.fillStyle = '#18181B';
  ctx.font = 'bold 20px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
  ctx.textAlign = 'right';
  ctx.fillText('Authorized Signatory', A4_W - MARGIN, A4_H - 78);
  ctx.textAlign = 'left';
}

function saveCanvasesAsPdf(canvases: HTMLCanvasElement[], filename: string) {
  const pdf = new jsPDF({ orientation: 'portrait', unit: 'pt', format: 'a4' });
  canvases.forEach((canvas, idx) => {
    if (idx > 0) pdf.addPage();
    const imgData = canvas.toDataURL('image/png');
    pdf.addImage(imgData, 'PNG', 0, 0, 595, 842);
  });
  pdf.save(filename);
}

export function downloadPaymentReceiptPdf(
  tx: LedgerTransaction,
  project: Project | undefined,
  client: Client | undefined,
  totalPaidForProject: number,
  strings: AppStrings
) {
  const [canvas, ctx] = createPageCanvas();
  const ref = tx.referenceNo || tx.id.slice(-6).toUpperCase();
  let y = drawBrandHeader(ctx, strings.paymentReceipt, ref);

  const drawKV = (x: number, yy: number, label: string, val: string) => {
    ctx.fillStyle = '#71717A';
    ctx.font = '18px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
    ctx.fillText(label, x, yy);
    ctx.fillStyle = '#18181B';
    ctx.font = 'bold 23px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
    ctx.fillText(val, x, yy + 30);
  };

  drawKV(MARGIN, y, strings.dateLabel, formatDate(tx.dateMillis));
  drawKV(640, y, strings.referenceNo, ref);
  y += 86;

  drawKV(MARGIN, y, strings.clientName, client?.name || tx.partyName);
  drawKV(640, y, strings.paymentMode, paymentModeLabel(tx.paymentMode, strings));
  y += 86;

  if (project) {
    drawKV(MARGIN, y, strings.selectProject, `${project.name} (${project.location})`);
    const pending = Math.max(0, project.contractValue - totalPaidForProject);
    drawKV(640, y, strings.pendingBalance, formatInr(pending, true));
    y += 96;
  }

  ctx.fillStyle = '#F7F5F0';
  ctx.fillRect(MARGIN, y, A4_W - MARGIN * 2, 144);
  ctx.strokeStyle = '#9A3412';
  ctx.lineWidth = 3;
  ctx.strokeRect(MARGIN, y, A4_W - MARGIN * 2, 144);

  ctx.fillStyle = '#71717A';
  ctx.font = 'bold 18px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
  ctx.fillText(strings.totalReceived.toUpperCase(), MARGIN + 28, y + 44);

  ctx.fillStyle = '#0F766E';
  ctx.font = 'bold 46px "IBM Plex Mono", "Plus Jakarta Sans", sans-serif';
  ctx.fillText(formatInr(tx.amount, true), MARGIN + 28, y + 106);
  y += 192;

  ctx.fillStyle = '#71717A';
  ctx.font = '18px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
  ctx.fillText(strings.titleDescLabel, MARGIN, y);
  y += 32;
  ctx.fillStyle = '#18181B';
  ctx.font = '24px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
  ctx.fillText(tx.title, MARGIN, y);

  if (tx.notes) {
    y += 38;
    ctx.fillStyle = '#52525B';
    ctx.font = '20px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
    ctx.fillText(`${strings.notes}: ${tx.notes}`, MARGIN, y);
  }

  drawFooter(ctx);
  saveCanvasesAsPdf([canvas], `Receipt_${ref}_${formatFileStamp()}.pdf`);
}

export function downloadProjectInvoicePdf(
  project: Project,
  client: Client | undefined,
  transactions: LedgerTransaction[],
  strings: AppStrings
) {
  const pages: HTMLCanvasElement[] = [];
  let [canvas, ctx] = createPageCanvas();
  pages.push(canvas);

  let y = drawBrandHeader(ctx, strings.projectInvoice, `INV-${project.id.slice(-6).toUpperCase()}`);

  const drawKV = (x: number, yy: number, label: string, val: string, valColor = '#18181B') => {
    ctx.fillStyle = '#71717A';
    ctx.font = '18px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
    ctx.fillText(label, x, yy);
    ctx.fillStyle = valColor;
    ctx.font = 'bold 22px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
    ctx.fillText(val, x, yy + 28);
  };

  drawKV(MARGIN, y, strings.selectProject, `${project.name} · ${project.location}`);
  drawKV(680, y, strings.dateLabel, formatDate(Date.now()));
  y += 74;

  drawKV(MARGIN, y, strings.clientName, client?.name || 'Direct Client');
  if (client?.phone) {
    drawKV(680, y, strings.phone, client.phone);
  }
  y += 74;

  const totalReceived = transactions
    .filter((t) => t.type === TransactionType.INCOME_CUSTOMER_PAYMENT)
    .reduce((sum, t) => sum + t.amount, 0);
  const totalExpenses = transactions
    .filter((t) => t.type !== TransactionType.INCOME_CUSTOMER_PAYMENT)
    .reduce((sum, t) => sum + t.amount, 0);
  const pending = Math.max(0, project.contractValue - totalReceived);

  ctx.fillStyle = '#F7F5F0';
  ctx.fillRect(MARGIN, y, A4_W - MARGIN * 2, 102);
  drawKV(MARGIN + 22, y + 32, strings.totalContract, formatInr(project.contractValue));
  drawKV(MARGIN + 360, y + 32, strings.totalReceived, formatInr(totalReceived), '#0F766E');
  drawKV(MARGIN + 700, y + 32, strings.pendingBalance, formatInr(pending), '#B91C1C');
  y += 134;

  const drawTableHeader = (yy: number): number => {
    ctx.fillStyle = '#EFECE4';
    ctx.fillRect(MARGIN, yy, A4_W - MARGIN * 2, 42);
    ctx.fillStyle = '#52525B';
    ctx.font = 'bold 17px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
    ctx.fillText('DATE', MARGIN + 12, yy + 27);
    ctx.fillText('CATEGORY', MARGIN + 150, yy + 27);
    ctx.fillText('PARTICULARS', MARGIN + 370, yy + 27);
    ctx.fillText('PARTY', MARGIN + 710, yy + 27);
    ctx.textAlign = 'right';
    ctx.fillText('AMOUNT (INR)', A4_W - MARGIN - 12, yy + 27);
    ctx.textAlign = 'left';
    return yy + 52;
  };

  y = drawTableHeader(y);

  const sorted = [...transactions].sort((a, b) => a.dateMillis - b.dateMillis);
  for (const tx of sorted) {
    if (y > A4_H - 170) {
      drawFooter(ctx);
      [canvas, ctx] = createPageCanvas();
      pages.push(canvas);
      y = drawTableHeader(80);
    }
    const isIncome = tx.type === TransactionType.INCOME_CUSTOMER_PAYMENT;
    ctx.fillStyle = '#18181B';
    ctx.font = '18px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
    ctx.fillText(formatDate(tx.dateMillis), MARGIN + 12, y + 22);
    ctx.fillText(categoryLabel(tx.category, strings).slice(0, 20), MARGIN + 150, y + 22);
    ctx.font = 'bold 18px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
    ctx.fillText(tx.title.slice(0, 32), MARGIN + 370, y + 22);
    ctx.font = '18px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
    ctx.fillText(tx.partyName.slice(0, 18), MARGIN + 710, y + 22);

    ctx.textAlign = 'right';
    ctx.fillStyle = isIncome ? '#0F766E' : '#18181B';
    ctx.font = 'bold 19px "IBM Plex Mono", monospace';
    ctx.fillText(`${isIncome ? '+' : '-'} ${formatInr(tx.amount)}`, A4_W - MARGIN - 12, y + 22);
    ctx.textAlign = 'left';

    ctx.strokeStyle = '#E2DDD2';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(MARGIN, y + 36);
    ctx.lineTo(A4_W - MARGIN, y + 36);
    ctx.stroke();
    y += 44;
  }

  y += 28;
  ctx.fillStyle = '#52525B';
  ctx.font = 'bold 20px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
  ctx.fillText(
    `${strings.totalExpenses}: ${formatInr(totalExpenses)}   |   ${strings.netProfitLoss}: ${formatInr(totalReceived - totalExpenses)}`,
    MARGIN,
    y
  );

  drawFooter(ctx);
  saveCanvasesAsPdf(pages, `Invoice_${project.name.replace(/\s+/g, '_')}_${formatFileStamp()}.pdf`);
}

export function downloadProfitLossPdf(
  scopeName: string,
  periodLabel: string,
  totalIncome: number,
  breakdown: { label: string; amount: number }[],
  strings: AppStrings
) {
  const [canvas, ctx] = createPageCanvas();
  let y = drawBrandHeader(ctx, strings.profitLossReport.toUpperCase(), `PNL-${formatFileStamp()}`);

  ctx.fillStyle = '#52525B';
  ctx.font = 'bold 20px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
  ctx.fillText(`${scopeName} · ${periodLabel}`, MARGIN, y);
  y += 50;

  const totalExpense = breakdown.reduce((s, item) => s + item.amount, 0);
  const netProfit = totalIncome - totalExpense;
  const margin = totalIncome > 0 ? (netProfit / totalIncome) * 100 : 0;

  const rows = [
    { label: strings.totalReceived, amount: totalIncome, positive: true },
    ...breakdown.map((b) => ({ label: b.label, amount: -b.amount, positive: false })),
  ];

  for (const row of rows) {
    ctx.fillStyle = '#18181B';
    ctx.font = '21px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
    ctx.fillText(row.label, MARGIN + 12, y + 26);

    ctx.textAlign = 'right';
    ctx.fillStyle = row.positive ? '#0F766E' : '#18181B';
    ctx.font = 'bold 22px "IBM Plex Mono", monospace';
    ctx.fillText(formatInr(row.amount, true), A4_W - MARGIN - 12, y + 26);
    ctx.textAlign = 'left';

    ctx.strokeStyle = '#E2DDD2';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(MARGIN, y + 46);
    ctx.lineTo(A4_W - MARGIN, y + 46);
    ctx.stroke();
    y += 56;
  }

  y += 28;
  ctx.fillStyle = '#F7F5F0';
  ctx.fillRect(MARGIN, y, A4_W - MARGIN * 2, 126);

  ctx.fillStyle = '#71717A';
  ctx.font = 'bold 18px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
  ctx.fillText(strings.totalExpenses.toUpperCase(), MARGIN + 24, y + 42);
  ctx.fillStyle = '#18181B';
  ctx.font = 'bold 30px "IBM Plex Mono", monospace';
  ctx.fillText(formatInr(totalExpense, true), MARGIN + 24, y + 90);

  ctx.fillStyle = '#71717A';
  ctx.font = 'bold 18px "Plus Jakarta Sans", "Noto Sans Tamil", sans-serif';
  ctx.fillText(`${strings.netProfitLoss.toUpperCase()} (${margin.toFixed(1)}%)`, 620, y + 42);
  ctx.fillStyle = netProfit >= 0 ? '#0F766E' : '#B91C1C';
  ctx.font = 'bold 34px "IBM Plex Mono", monospace';
  ctx.fillText(formatInr(netProfit, true), 620, y + 92);

  drawFooter(ctx);
  saveCanvasesAsPdf([canvas], `Chola_PnL_${formatFileStamp()}.pdf`);
}
