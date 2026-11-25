import React, { useState } from 'react';
import { X } from 'lucide-react';
import { AppStrings, projectStatusLabel } from '../i18n/strings';
import { Client, Project, ProjectStatus, SyncStatus } from '../types/accounts';

interface ProjectModalProps {
  existing: Project | null;
  clients: Client[];
  strings: AppStrings;
  onClose: () => void;
  onSave: (project: Project) => void;
}

export const ProjectModal: React.FC<ProjectModalProps> = ({
  existing,
  clients,
  strings,
  onClose,
  onSave,
}) => {
  const [name, setName] = useState(existing?.name ?? '');
  const [clientId, setClientId] = useState(existing?.clientId ?? clients[0]?.id ?? '');
  const [location, setLocation] = useState(existing?.location ?? '');
  const [contractValue, setContractValue] = useState(
    existing?.contractValue ? String(existing.contractValue) : ''
  );
  const [status, setStatus] = useState<ProjectStatus>(existing?.status ?? ProjectStatus.ACTIVE);
  const [notes, setNotes] = useState(existing?.notes ?? '');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    onSave({
      id: existing?.id ?? `proj_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
      name: name.trim(),
      clientId,
      location: location.trim(),
      contractValue: Math.max(0, parseFloat(contractValue) || 0),
      status,
      notes: notes.trim(),
      createdAt: existing?.createdAt ?? Date.now(),
      syncStatus: SyncStatus.LOCAL_ONLY,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
      <div className="bg-white border border-[#e2ddd2] rounded-xl w-full max-w-md p-6">
        <div className="flex items-center justify-between pb-3 border-b border-[#e2ddd2]">
          <h3 className="text-lg font-semibold text-zinc-900">
            {existing ? strings.editProject : strings.addProject}
          </h3>
          <button type="button" onClick={onClose} className="p-1.5 text-zinc-500 hover:bg-zinc-100 rounded-lg">
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-3">
          <div>
            <label className="block text-xs font-medium text-zinc-600 mb-1">
              {strings.projectName} *
            </label>
            <input
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3 py-2 text-sm border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-zinc-600 mb-1">
              {strings.selectClient}
            </label>
            <select
              value={clientId}
              onChange={(e) => setClientId(e.target.value)}
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

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-zinc-600 mb-1">
                {strings.location}
              </label>
              <input
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-zinc-600 mb-1">
                {strings.contractValue}
              </label>
              <input
                type="number"
                min="0"
                step="any"
                value={contractValue}
                onChange={(e) => setContractValue(e.target.value)}
                className="w-full px-3 py-2 text-sm font-mono-num border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-zinc-600 mb-1">
              {strings.status}
            </label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as ProjectStatus)}
              className="w-full px-3 py-2 text-sm bg-white border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
            >
              {Object.values(ProjectStatus).map((st) => (
                <option key={st} value={st}>
                  {projectStatusLabel(st, strings)}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-zinc-600 mb-1">
              {strings.notes}
            </label>
            <input
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full px-3 py-2 text-sm border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
            />
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-[#e2ddd2]">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-zinc-700 border border-[#e2ddd2] rounded-lg hover:bg-[#f7f5f0]"
            >
              {strings.cancel}
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-sm font-medium bg-[#9a3412] text-white rounded-lg hover:bg-[#7c2d12]"
            >
              {strings.save}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

interface ClientModalProps {
  existing: Client | null;
  strings: AppStrings;
  onClose: () => void;
  onSave: (client: Client) => void;
}

export const ClientModal: React.FC<ClientModalProps> = ({
  existing,
  strings,
  onClose,
  onSave,
}) => {
  const [name, setName] = useState(existing?.name ?? '');
  const [phone, setPhone] = useState(existing?.phone ?? '');
  const [address, setAddress] = useState(existing?.address ?? '');
  const [gstNumber, setGstNumber] = useState(existing?.gstNumber ?? '');
  const [notes, setNotes] = useState(existing?.notes ?? '');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    onSave({
      id: existing?.id ?? `client_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
      name: name.trim(),
      phone: phone.trim(),
      address: address.trim(),
      gstNumber: gstNumber.trim(),
      notes: notes.trim(),
      createdAt: existing?.createdAt ?? Date.now(),
      syncStatus: SyncStatus.LOCAL_ONLY,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
      <div className="bg-white border border-[#e2ddd2] rounded-xl w-full max-w-md p-6">
        <div className="flex items-center justify-between pb-3 border-b border-[#e2ddd2]">
          <h3 className="text-lg font-semibold text-zinc-900">
            {existing ? strings.editClient : strings.addClient}
          </h3>
          <button type="button" onClick={onClose} className="p-1.5 text-zinc-500 hover:bg-zinc-100 rounded-lg">
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-3">
          <div>
            <label className="block text-xs font-medium text-zinc-600 mb-1">
              {strings.clientName} *
            </label>
            <input
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3 py-2 text-sm border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-zinc-600 mb-1">
              {strings.phone}
            </label>
            <input
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="w-full px-3 py-2 text-sm border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-zinc-600 mb-1">
              {strings.address}
            </label>
            <input
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              className="w-full px-3 py-2 text-sm border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-zinc-600 mb-1">
              {strings.gstNumber}
            </label>
            <input
              value={gstNumber}
              onChange={(e) => setGstNumber(e.target.value)}
              className="w-full px-3 py-2 text-sm border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-zinc-600 mb-1">
              {strings.notes}
            </label>
            <input
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full px-3 py-2 text-sm border border-[#e2ddd2] rounded-lg focus:outline-none focus:border-[#9a3412]"
            />
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-[#e2ddd2]">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-zinc-700 border border-[#e2ddd2] rounded-lg hover:bg-[#f7f5f0]"
            >
              {strings.cancel}
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-sm font-medium bg-[#9a3412] text-white rounded-lg hover:bg-[#7c2d12]"
            >
              {strings.save}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
