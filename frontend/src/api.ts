export interface Group {
  id: string;
  name: string;
  schedule: string;
  timezone: string;
  createdAt: string;
}

export interface Member {
  id: string;
  groupId: string;
  name: string;
  channel: string;
  joinedAt: string;
}

export interface HistoryEntry {
  phrase: string;
}

const BASE = '';

async function handleResponse<T>(res: Response): Promise<T> {
  if (res.status === 401 || res.status === 403) {
    // Not authenticated or not allowed — redirect to OAuth
    window.location.href = '/oauth2/authorization/google';
    throw new Error('Redirecting to login');
  }
  if (!res.ok) {
    throw new Error(`API error: ${res.status}`);
  }
  return res.json();
}

export async function fetchGroups(): Promise<Group[]> {
  const res = await fetch(`${BASE}/api/groups`);
  return handleResponse(res);
}

export async function createGroup(name: string, schedule: string, timezone: string): Promise<Group> {
  const res = await fetch(`${BASE}/api/groups`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, schedule, timezone }),
  });
  return handleResponse(res);
}

export async function fetchMembers(groupId: string): Promise<Member[]> {
  const res = await fetch(`${BASE}/api/groups/${groupId}/members`);
  return handleResponse(res);
}

export async function joinGroup(groupId: string, name: string, ntfyTopic: string): Promise<Member> {
  const res = await fetch(`${BASE}/api/groups/${groupId}/members`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, channel: `ntfy:${ntfyTopic}` }),
  });
  return handleResponse(res);
}

export async function fetchHistory(groupId: string): Promise<HistoryEntry[]> {
  const res = await fetch(`${BASE}/api/groups/${groupId}/history`);
  return handleResponse(res);
}

export async function triggerRotate(): Promise<{ rotated: number }> {
  const res = await fetch(`${BASE}/api/rotate`, { method: 'POST' });
  return handleResponse(res);
}
