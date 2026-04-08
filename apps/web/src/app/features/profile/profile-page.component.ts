import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { distinctUntilChanged, filter } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import {
  ProfileApiService,
  type DayCategory,
  type HolidaySyncStatus,
  type ProfileResponse,
  type VacationPeriod,
  type ZoneImpactResponse
} from './profile-api.service';

type GeographicZone = 'METROPOLE' | 'ALSACE_LORRAINE';

@Component({
  selector: 'app-profile-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './profile-page.component.html',
  styleUrl: './profile-page.component.scss'
})
export class ProfilePageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly profileApi = inject(ProfileApiService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly zoneImpact = signal<ZoneImpactResponse | null>(null);
  protected readonly profilePhotoUrl = signal<string | null>(null);
  protected readonly selectedPhotoName = signal('');
  protected readonly tokenMissing = signal(false);
  protected readonly loggingOut = signal(false);
  protected readonly syncAlert = signal<HolidaySyncStatus | null>(null);
  protected readonly vacationPeriods = signal<VacationPeriod[]>([]);
  protected readonly vacationSubmitting = signal(false);
  protected readonly vacationErrorMessage = signal('');
  protected readonly editingVacationId = signal<string | null>(null);
  protected readonly deleting = signal(false);
  protected readonly deletionErrorMessage = signal('');

  private originalZone: GeographicZone | null = null;
  private profilePhotoFile: File | null = null;

  protected readonly form = this.fb.nonNullable.group({
    pseudo: ['', [Validators.required, Validators.maxLength(100)]],
    geographicZone: ['METROPOLE' as GeographicZone, Validators.required],
    timezoneName: ['Europe/Paris', [Validators.required, Validators.pattern(/^[A-Za-z]+\/[A-Za-z_]+$/)]],
    zoneChangeConfirmed: [false],
    workdayWakeUpTime: ['07:00', [Validators.required, Validators.pattern(/^([01]\d|2[0-3]):[0-5]\d$/)]],
    vacationWakeUpTime: ['09:00', [Validators.required, Validators.pattern(/^([01]\d|2[0-3]):[0-5]\d$/)]],
    weekendHolidayWakeUpTime: ['08:30', [Validators.required, Validators.pattern(/^([01]\d|2[0-3]):[0-5]\d$/)]]
  });

  protected readonly vacationForm = this.fb.nonNullable.group({
    label: ['', [Validators.required, Validators.maxLength(100)]],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required]
  });

  protected readonly deletionForm = this.fb.nonNullable.group({
    confirmPassword: ['', Validators.required]
  });

  constructor() {
    this.watchZoneChanges();
    this.loadProfile();
  }

  protected onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0) ?? null;
    this.profilePhotoFile = file;
    this.selectedPhotoName.set(file?.name ?? '');
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.requiresZoneConfirmation() && !this.form.controls.zoneChangeConfirmed.value) {
      this.errorMessage.set("Confirme d'abord l'impact sur les jours fériés.");
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const value = this.form.getRawValue();
    const payload = new FormData();
    payload.set('pseudo', value.pseudo);
    payload.set('geographicZone', value.geographicZone);
    payload.set('timezoneName', value.timezoneName);
    payload.set('zoneChangeConfirmed', String(value.zoneChangeConfirmed));
    this.appendDayProfile(payload, 0, 'WORKDAY', value.workdayWakeUpTime);
    this.appendDayProfile(payload, 1, 'VACATION', value.vacationWakeUpTime);
    this.appendDayProfile(payload, 2, 'WEEKEND_HOLIDAY', value.weekendHolidayWakeUpTime);

    if (this.profilePhotoFile) {
      payload.set('profilePhoto', this.profilePhotoFile);
    }

    this.profileApi.updateProfile(payload).subscribe({
      next: ({ data }) => {
        this.applyProfile(data);
        this.successMessage.set('Profil mis à jour.');
        this.submitting.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error?.error?.error?.message ?? 'Impossible de mettre à jour le profil.');
        this.submitting.set(false);
      }
    });
  }

  protected controlInvalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  protected vacationControlInvalid(controlName: keyof typeof this.vacationForm.controls): boolean {
    const control = this.vacationForm.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  protected requiresZoneConfirmation(): boolean {
    return !!this.zoneImpact()?.holidayRulesWillChange && this.form.controls.geographicZone.value !== this.originalZone;
  }

  protected editVacation(vacation: VacationPeriod): void {
    this.editingVacationId.set(vacation.id);
    this.vacationErrorMessage.set('');
    this.vacationForm.reset({
      label: vacation.label,
      startDate: vacation.startDate,
      endDate: vacation.endDate
    });
  }

  protected cancelVacationEdit(): void {
    this.editingVacationId.set(null);
    this.vacationErrorMessage.set('');
    this.vacationForm.reset({
      label: '',
      startDate: '',
      endDate: ''
    });
  }

  protected saveVacation(): void {
    if (this.vacationForm.invalid || this.vacationSubmitting()) {
      this.vacationForm.markAllAsTouched();
      return;
    }

    const value = this.vacationForm.getRawValue();
    if (new Date(value.endDate) < new Date(value.startDate)) {
      this.vacationErrorMessage.set('La date de fin doit être postérieure ou égale à la date de début.');
      return;
    }

    const newStart = new Date(value.startDate);
    const newEnd = new Date(value.endDate);
    const editingId = this.editingVacationId();
    const hasOverlap = this.vacationPeriods().some((period) => {
      if (period.id === editingId) return false;
      return newStart <= new Date(period.endDate) && newEnd >= new Date(period.startDate);
    });
    if (hasOverlap) {
      this.vacationErrorMessage.set('Cette période chevauche une période de vacances existante.');
      return;
    }

    this.vacationSubmitting.set(true);
    this.vacationErrorMessage.set('');
    this.successMessage.set('');

    const payload = {
      label: value.label.trim(),
      startDate: value.startDate,
      endDate: value.endDate
    };
    const request$ = editingId
      ? this.profileApi.updateVacation(editingId, payload)
      : this.profileApi.createVacation(payload);

    request$.subscribe({
      next: ({ data }) => {
        if (editingId) {
          this.vacationPeriods.update((items) => items.map((item) => (item.id === data.id ? data : item)));
          this.successMessage.set('Période de vacances mise à jour.');
        } else {
          this.vacationPeriods.update((items) =>
            [...items, data].sort((left, right) => left.startDate.localeCompare(right.startDate))
          );
          this.successMessage.set('Période de vacances ajoutée.');
        }
        this.vacationSubmitting.set(false);
        this.cancelVacationEdit();
      },
      error: (error) => {
        this.vacationErrorMessage.set(
          error?.error?.error?.message ?? "Impossible d'enregistrer la période de vacances."
        );
        this.vacationSubmitting.set(false);
      }
    });
  }

  protected deleteVacation(vacation: VacationPeriod): void {
    if (this.vacationSubmitting()) {
      return;
    }

    this.vacationSubmitting.set(true);
    this.vacationErrorMessage.set('');
    this.successMessage.set('');

    this.profileApi.deleteVacation(vacation.id).subscribe({
      next: () => {
        this.vacationPeriods.update((items) => items.filter((item) => item.id !== vacation.id));
        if (this.editingVacationId() === vacation.id) {
          this.cancelVacationEdit();
        }
        this.successMessage.set('Période de vacances supprimée.');
        this.vacationSubmitting.set(false);
      },
      error: (error) => {
        this.vacationErrorMessage.set(error?.error?.error?.message ?? 'Impossible de supprimer cette période.');
        this.vacationSubmitting.set(false);
      }
    });
  }

  protected syncStatusLabel(): string {
    switch (this.syncAlert()?.status) {
      case 'SYNCED':
        return 'Synchronisation OK';
      case 'RETRY_SCHEDULED':
        return 'Nouvelle tentative planifiée';
      case 'FAILED':
        return 'Synchronisation en échec';
      default:
        return 'Synchronisation en attente';
    }
  }

  protected logout(): void {
    if (this.loggingOut()) {
      return;
    }

    this.loggingOut.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.profileApi.logout().subscribe({
      next: async () => {
        this.authService.clearAccessToken();
        this.loggingOut.set(false);
        await this.router.navigateByUrl('/login');
      },
      error: () => {
        this.authService.clearAccessToken();
        this.loggingOut.set(false);
        void this.router.navigateByUrl('/login');
      }
    });
  }

  protected deletionControlInvalid(controlName: keyof typeof this.deletionForm.controls): boolean {
    const control = this.deletionForm.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  protected deleteAccount(): void {
    if (this.deletionForm.invalid || this.deleting()) {
      this.deletionForm.markAllAsTouched();
      return;
    }

    this.deleting.set(true);
    this.deletionErrorMessage.set('');

    const { confirmPassword } = this.deletionForm.getRawValue();

    this.profileApi.deleteAccount(confirmPassword).subscribe({
      next: async () => {
        this.authService.clearAccessToken();
        this.deleting.set(false);
        await this.router.navigateByUrl('/login');
      },
      error: (error) => {
        this.deletionErrorMessage.set(
          error?.error?.error?.message ?? 'Impossible de supprimer le compte.'
        );
        this.deleting.set(false);
        this.deletionForm.reset({ confirmPassword: '' });
      }
    });
  }

  private loadProfile(): void {
    if (!this.authService.isAuthenticated()) {
      this.tokenMissing.set(true);
      this.loading.set(false);
      return;
    }

    this.profileApi.getProfile().subscribe({
      next: ({ data }) => {
        this.applyProfile(data);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error?.error?.error?.message ?? 'Impossible de charger le profil.');
        this.loading.set(false);
      }
    });
  }

  private applyProfile(profile: ProfileResponse): void {
    this.originalZone = profile.geographicZone;
    this.tokenMissing.set(false);
    this.profilePhotoUrl.set(profile.profilePhotoSignedUrl);
    this.selectedPhotoName.set('');
    this.profilePhotoFile = null;
    this.zoneImpact.set(null);
    this.syncAlert.set(profile.holidaySyncStatus);
    this.vacationPeriods.set(profile.vacationPeriods);
    this.cancelVacationEdit();

    const dayProfiles = new Map(profile.dayProfiles.map((dayProfile) => [dayProfile.dayCategory, dayProfile.wakeUpTime]));
    this.form.reset({
      pseudo: profile.pseudo,
      geographicZone: profile.geographicZone,
      timezoneName: profile.timezoneName,
      zoneChangeConfirmed: false,
      workdayWakeUpTime: dayProfiles.get('WORKDAY') ?? '07:00',
      vacationWakeUpTime: dayProfiles.get('VACATION') ?? '09:00',
      weekendHolidayWakeUpTime: dayProfiles.get('WEEKEND_HOLIDAY') ?? '08:30'
    });
  }

  private watchZoneChanges(): void {
    this.form.controls.geographicZone.valueChanges
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        distinctUntilChanged(),
        filter((zone): zone is GeographicZone => !!zone)
      )
      .subscribe((zone) => {
        if (!this.originalZone || zone === this.originalZone) {
          this.zoneImpact.set(null);
          this.form.controls.zoneChangeConfirmed.setValue(false, { emitEvent: false });
          return;
        }

        this.profileApi.previewZoneImpact(zone).subscribe({
          next: ({ data }) => {
            this.zoneImpact.set(data);
            this.form.controls.zoneChangeConfirmed.setValue(false, { emitEvent: false });
          },
          error: () => {
            this.errorMessage.set("Impossible d'évaluer l'impact du changement de zone.");
          }
        });
      });
  }

  private appendDayProfile(payload: FormData, index: number, dayCategory: DayCategory, wakeUpTime: string): void {
    payload.set(`dayProfiles[${index}].dayCategory`, dayCategory);
    payload.set(`dayProfiles[${index}].wakeUpTime`, wakeUpTime);
  }
}
