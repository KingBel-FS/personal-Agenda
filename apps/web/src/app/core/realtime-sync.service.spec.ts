import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { RealtimeSyncService } from './realtime-sync.service';

class FakeEventSource {
  static latest: FakeEventSource | null = null;

  onerror: (() => void) | null = null;
  private readonly listeners = new Map<string, Array<(event: MessageEvent<string>) => void>>();

  constructor(public readonly url: string) {
    FakeEventSource.latest = this;
  }

  addEventListener(type: string, listener: (event: MessageEvent<string>) => void): void {
    const listeners = this.listeners.get(type) ?? [];
    listeners.push(listener);
    this.listeners.set(type, listeners);
  }

  emit(type: string, payload: unknown): void {
    const event = { data: JSON.stringify(payload) } as MessageEvent<string>;
    for (const listener of this.listeners.get(type) ?? []) {
      listener(event);
    }
  }

  close(): void {}
}

describe('RealtimeSyncService', () => {
  const originalEventSource = globalThis.EventSource;

  beforeEach(() => {
    (globalThis as typeof globalThis & { EventSource: typeof EventSource }).EventSource = FakeEventSource as unknown as typeof EventSource;
    TestBed.configureTestingModule({
      providers: [AuthService, RealtimeSyncService]
    });
  });

  afterEach(() => {
    (globalThis as typeof globalThis & { EventSource: typeof EventSource }).EventSource = originalEventSource;
    localStorage.removeItem('pht_access_token');
    FakeEventSource.latest = null;
  });

  it('emits sync events received from the SSE stream', () => {
    const authService = TestBed.inject(AuthService);
    authService.setAccessToken('test-token');
    const service = TestBed.inject(RealtimeSyncService);

    let receivedScope = '';
    service.events$.subscribe((event) => {
      receivedScope = event.scope;
    });

    service.start();
    FakeEventSource.latest?.emit('sync', { scope: 'TODAY', occurredAt: '2026-03-27T12:00:00Z' });

    expect(receivedScope).toBe('TODAY');
    expect(FakeEventSource.latest?.url).toContain('/api/v1/sync/events?access_token=test-token');
  });
});
