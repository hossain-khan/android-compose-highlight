interface Repository<T extends { id: number }> {
  findById(id: number): Promise<T | null>;
  save(entity: Omit<T, 'id'>): Promise<T>;
  delete(id: number): Promise<void>;
}

type ApiResponse<T> = {
  data: T;
  status: number;
  message?: string;
};

async function fetchWithRetry<T>(
  url: string,
  retries = 3,
): Promise<ApiResponse<T>> {
  for (let attempt = 0; attempt < retries; attempt++) {
    try {
      const res = await fetch(url);
      if (!res.ok) throw new Error(`HTTP ${'$'}{res.status}: ${'$'}{res.statusText}`);
      const data: T = await res.json();
      return { data, status: res.status };
    } catch (err) {
      if (attempt === retries - 1) throw err;
      await new Promise(r => setTimeout(r, 2 ** attempt * 100));
    }
  }
  throw new Error('unreachable');
}