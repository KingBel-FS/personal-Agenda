import { Location } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EmojiPickerComponent } from './emoji-picker.component';
import { TodayApiService } from '../today/today-api.service';
import {
  TaskApiService,
  type DeleteTaskOccurrenceRequest,
  type TaskOccurrence,
  type TaskOccurrenceListQuery,
  type TimeSlotSummary,
  type UpdateTaskOccurrenceRequest
} from './task-api.service';

type PendingAction = 'edit' | 'delete';
type MutationScope = 'THIS_OCCURRENCE' | 'THIS_AND_FOLLOWING';
type DayCategory = 'WORKDAY' | 'VACATION' | 'WEEKEND_HOLIDAY';
type RecurrenceType = 'WEEKLY' | 'MONTHLY';
type TaskKindFilter = 'ALL' | 'ONE_TIME' | 'RECURRING_AFTER_DATE';
type TimeModeFilter = 'ALL' | 'FIXED' | 'WAKE_UP_OFFSET';

const DAY_CATEGORIES = ['WORKDAY', 'VACATION', 'WEEKEND_HOLIDAY'] as const;
const DAY_CATEGORY_LABELS: Record<DayCategory, string> = {
  WORKDAY: 'Travail',
  VACATION: 'Vacances',
  WEEKEND_HOLIDAY: 'Week-end et ferie'
};
const DAYS_OF_WEEK = [
  { value: 1, label: 'Lundi' },
  { value: 2, label: 'Mardi' },
  { value: 3, label: 'Mercredi' },
  { value: 4, label: 'Jeudi' },
  { value: 5, label: 'Vendredi' },
  { value: 6, label: 'Samedi' },
  { value: 7, label: 'Dimanche' }
] as const;

@Component({
  selector: 'app-task-manage-page',
  imports: [ReactiveFormsModule, RouterLink, EmojiPickerComponent],
  templateUrl: './task-manage-page.component.html',
  styleUrl: './task-manage-page.component.scss'
})
export class TaskManagePageComponent {
  private readonly location = inject(Location);
  private readonly fb = inject(FormBuilder);
  private readonly taskApi = inject(TaskApiService);
  private readonly todayApi = inject(TodayApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly toastMessage = signal('');
  protected readonly selectedPhotoName = signal('');
  protected readonly occurrences = signal<TaskOccurrence[]>([]);
  protected readonly currentPage = signal(0);
  protected readonly pageSize = signal(10);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly selectedOccurrence = signal<TaskOccurrence | null>(null);
  protected readonly pendingAction = signal<PendingAction | null>(null);
  protected readonly selectedScope = signal<MutationScope>('THIS_OCCURRENCE');
  protected readonly scopeSheetOpen = signal(false);
  protected readonly showEmojiPicker = signal(false);
  protected readonly slotsModified = signal(false);
  protected readonly dayCategoryLabels = DAY_CATEGORY_LABELS;
  protected readonly dayCategoryKeys = DAY_CATEGORIES;
  protected readonly daysOfWeek = DAYS_OF_WEEK;
  protected readonly todayIso = this.toIsoDate(new Date());
  protected readonly pastStatus = signal<'planned' | 'done' | 'missed'>('planned');

  private selectedPhotoFile: File | null = null;
  private autoOpenOccurrenceId: string | null = null;
  private autoOpenScope: MutationScope = 'THIS_OCCURRENCE';
  private autoOpenAction: PendingAction = 'edit';
  private autoOpened = false;

  protected readonly filterForm = this.fb.nonNullable.group({
    search: [''],
    taskKind: ['ALL' as TaskKindFilter],
    selectedDate: [this.todayIso],
    occurrenceDateFrom: [this.todayIso],
    occurrenceDateTo: [''],
    timeMode: ['ALL' as TimeModeFilter],
    fixedTime: [''],
    wakeUpOffsetHours: [''],
    wakeUpOffsetMins: ['']
  });

  protected readonly editForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(100)]],
    icon: ['', [Validators.required, Validators.maxLength(50)]],
    description: [''],
    timeMode: ['FIXED' as 'FIXED' | 'WAKE_UP_OFFSET', Validators.required],
    fixedTime: ['08:00'],
    wakeUpOffsetHours: [0],
    wakeUpOffsetMins: [30],
    WORKDAY: [false],
    VACATION: [false],
    WEEKEND_HOLIDAY: [false],
    recurrenceType: ['WEEKLY' as RecurrenceType],
    dow1: [false],
    dow2: [false],
    dow3: [false],
    dow4: [false],
    dow5: [false],
    dow6: [false],
    dow7: [false],
    dayOfMonth: [1],
    endDate: [''],
    endTime: ['']
  });

  protected readonly timeSlotsArray = this.fb.array<ReturnType<typeof this.buildSlotGroup>>([]);

  constructor() {
    this.bootstrapFromQueryParams();
    this.loadOccurrences();
  }

  protected get slots(): FormArray {
    return this.timeSlotsArray;
  }

  protected addTimeSlot(): void {
    this.timeSlotsArray.push(this.buildSlotGroup());
    this.slotsModified.set(true);
  }

  protected removeTimeSlot(index: number): void {
    this.timeSlotsArray.removeAt(index);
    this.slotsModified.set(true);
  }

  protected goBack(): void {
    this.location.back();
  }

  protected applyFilters(): void {
    this.currentPage.set(0);
    this.loadOccurrences();
  }

  protected resetFilters(): void {
    this.filterForm.reset({
      search: '',
      taskKind: 'ALL',
      selectedDate: this.todayIso,
      occurrenceDateFrom: this.todayIso,
      occurrenceDateTo: '',
      timeMode: 'ALL',
      fixedTime: '',
      wakeUpOffsetHours: '',
      wakeUpOffsetMins: ''
    });
    this.currentPage.set(0);
    this.loadOccurrences();
  }

  protected previousPage(): void {
    if (this.currentPage() === 0 || this.loading()) {
      return;
    }
    this.currentPage.update((page) => page - 1);
    this.loadOccurrences();
  }

  protected nextPage(): void {
    if (this.loading() || this.currentPage() + 1 >= this.totalPages()) {
      return;
    }
    this.currentPage.update((page) => page + 1);
    this.loadOccurrences();
  }

  protected onEmojiSelected(emoji: string): void {
    this.editForm.controls.icon.setValue(emoji);
    this.showEmojiPicker.set(false);
  }

  protected slotScheduleSummary(slot: ReturnType<typeof this.buildSlotGroup>): string {
    const mode = slot.controls.timeMode.value;
    if (mode === 'EVERY_N_MINUTES') {
      const hours = slot.controls.intervalHours.value;
      const minutes = slot.controls.intervalMins.value;
      return hours > 0 ? `Toutes les ${hours}h${minutes > 0 ? minutes + 'min' : ''}` : `Toutes les ${minutes} min`;
    }
    return `Heure fixe : ${slot.controls.fixedTime.value}`;
  }

  protected onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedPhotoFile = file;
    this.selectedPhotoName.set(file?.name ?? '');
  }

  protected openScopeSheet(action: PendingAction, occurrence: TaskOccurrence): void {
    this.successMessage.set('');
    this.errorMessage.set('');
    this.pendingAction.set(action);
    this.selectedOccurrence.set(occurrence);
    this.selectedScope.set('THIS_OCCURRENCE');
    this.scopeSheetOpen.set(true);
    this.showEmojiPicker.set(false);
    this.slotsModified.set(false);
    this.selectedPhotoFile = null;
    this.selectedPhotoName.set('');

    if (action === 'edit') {
      this.pastStatus.set(this.normalizePastStatus(occurrence.status));
      this.timeSlotsArray.clear();
      for (const slot of occurrence.timeSlots ?? []) {
        const group = this.buildSlotGroup(slot.timeMode === 'EVERY_N_MINUTES' ? 'EVERY_N_MINUTES' : 'FIXED');
        if (slot.timeMode === 'EVERY_N_MINUTES') {
          const minutes = slot.afterPreviousMinutes ?? 60;
          group.controls.intervalHours.setValue(Math.floor(minutes / 60));
          group.controls.intervalMins.setValue(minutes % 60);
        } else {
          group.controls.fixedTime.setValue(slot.fixedTime?.slice(0, 5) ?? '');
        }
        this.timeSlotsArray.push(group);
      }

      const offset = Math.max(0, occurrence.wakeUpOffsetMinutes ?? 0);
      this.editForm.reset({
        title: occurrence.title,
        icon: occurrence.icon,
        description: occurrence.description ?? '',
        timeMode: occurrence.timeMode,
        fixedTime: occurrence.fixedTime ? occurrence.fixedTime.slice(0, 5) : occurrence.occurrenceTime.slice(0, 5),
        wakeUpOffsetHours: Math.floor(offset / 60),
        wakeUpOffsetMins: offset % 60,
        WORKDAY: occurrence.dayCategories.includes('WORKDAY'),
        VACATION: occurrence.dayCategories.includes('VACATION'),
        WEEKEND_HOLIDAY: occurrence.dayCategories.includes('WEEKEND_HOLIDAY'),
        recurrenceType: (occurrence.recurrenceType as RecurrenceType | null) ?? 'WEEKLY',
        dow1: occurrence.daysOfWeek?.includes(1) ?? false,
        dow2: occurrence.daysOfWeek?.includes(2) ?? false,
        dow3: occurrence.daysOfWeek?.includes(3) ?? false,
        dow4: occurrence.daysOfWeek?.includes(4) ?? false,
        dow5: occurrence.daysOfWeek?.includes(5) ?? false,
        dow6: occurrence.daysOfWeek?.includes(6) ?? false,
        dow7: occurrence.daysOfWeek?.includes(7) ?? false,
        dayOfMonth: occurrence.dayOfMonth ?? 1,
        endDate: occurrence.endDate ?? '',
        endTime: occurrence.endTime ?? ''
      });
    }
  }

  protected closeScopeSheet(): void {
    this.scopeSheetOpen.set(false);
    this.pendingAction.set(null);
    this.selectedOccurrence.set(null);
    this.showEmojiPicker.set(false);
    this.timeSlotsArray.clear();
    this.selectedPhotoFile = null;
    this.selectedPhotoName.set('');
  }

  protected confirmMutation(): void {
    const occurrence = this.selectedOccurrence();
    const action = this.pendingAction();
    if (!occurrence || !action || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    if (action === 'edit' && occurrence.pastLocked) {
      this.confirmPastOccurrenceMutation(occurrence);
      return;
    }

    if (action === 'delete') {
      const payload: DeleteTaskOccurrenceRequest = { scope: this.selectedScope() };
      this.taskApi.deleteOccurrence(occurrence.id, payload).subscribe({
        next: ({ data }) => {
          this.successMessage.set(data.message);
          this.toastMessage.set('');
          this.saving.set(false);
          this.closeScopeSheet();
          this.clearEditorQueryParams();
          this.loadOccurrences();
        },
        error: (error) => {
          this.handleUiError(error, 'Impossible de supprimer cette occurrence.');
          this.saving.set(false);
        }
      });
      return;
    }

    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      this.saving.set(false);
      return;
    }

    if (this.selectedScope() === 'THIS_AND_FOLLOWING' && occurrence.recurring) {
      if (!this.selectedCategories().length) {
        this.handleUiError(
          { error: { error: { code: 'BUSINESS_RULE_VIOLATION', message: 'Selectionne au moins un type de jour pour la serie.' } } },
          'Selectionne au moins un type de jour pour la serie.'
        );
        this.saving.set(false);
        return;
      }
      if (this.editForm.controls.recurrenceType.value === 'WEEKLY' && !this.selectedDaysOfWeek().length) {
        this.handleUiError(
          { error: { error: { code: 'BUSINESS_RULE_VIOLATION', message: 'Selectionne au moins un jour de la semaine pour la serie.' } } },
          'Selectionne au moins un jour de la semaine pour la serie.'
        );
        this.saving.set(false);
        return;
      }
    }

    const value = this.editForm.getRawValue();
    const payload: UpdateTaskOccurrenceRequest = {
      scope: this.selectedScope(),
      title: value.title.trim(),
      icon: value.icon.trim(),
      description: value.description.trim() || null,
      timeMode: value.timeMode,
      fixedTime: value.timeMode === 'FIXED' ? value.fixedTime : null,
      wakeUpOffsetMinutes:
        value.timeMode === 'WAKE_UP_OFFSET'
          ? this.toWakeUpOffsetMinutes(value.wakeUpOffsetHours, value.wakeUpOffsetMins)
          : null,
      dayCategories: occurrence.recurring && this.selectedScope() === 'THIS_AND_FOLLOWING' ? this.selectedCategories() : null,
      recurrenceType: occurrence.recurring && this.selectedScope() === 'THIS_AND_FOLLOWING' ? value.recurrenceType : null,
      daysOfWeek:
        occurrence.recurring && this.selectedScope() === 'THIS_AND_FOLLOWING' && value.recurrenceType === 'WEEKLY'
          ? this.selectedDaysOfWeek()
          : null,
      dayOfMonth:
        occurrence.recurring && this.selectedScope() === 'THIS_AND_FOLLOWING' && value.recurrenceType === 'MONTHLY'
          ? value.dayOfMonth
          : null,
      endDate: occurrence.recurring && this.selectedScope() === 'THIS_AND_FOLLOWING' ? value.endDate || null : null,
      endTime: value.endTime || null,
      timeSlots:
        (this.selectedScope() === 'THIS_AND_FOLLOWING' || this.slotsModified())
          ? this.timeSlotsArray.getRawValue().map((slot) => ({
              timeMode: slot.timeMode,
              fixedTime: slot.timeMode === 'FIXED' ? slot.fixedTime || null : null,
              afterPreviousMinutes:
                slot.timeMode === 'EVERY_N_MINUTES'
                  ? Math.min(720, Math.max(1, slot.intervalHours * 60 + slot.intervalMins))
                  : null
            }))
          : null
    };

    this.taskApi.updateOccurrence(occurrence.id, payload).subscribe({
      next: ({ data }) => {
        if (!this.selectedPhotoFile) {
          this.onMutationCompleted();
          return;
        }

        this.taskApi.updateOccurrencePhoto(data.id, this.selectedScope(), this.selectedPhotoFile).subscribe({
          next: () => this.onMutationCompleted(),
          error: (error) => {
            this.handleUiError(error, 'Impossible de mettre a jour la photo.');
            this.saving.set(false);
          }
        });
      },
      error: (error) => {
        this.handleUiError(error, 'Impossible de mettre a jour cette occurrence.');
        this.saving.set(false);
      }
    });
  }

  protected dismissToast(): void {
    this.toastMessage.set('');
  }

  protected controlInvalid(controlName: keyof typeof this.editForm.controls): boolean {
    const control = this.editForm.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  protected dayCategoryLabel(category: string): string {
    return this.dayCategoryLabels[category as DayCategory] ?? category;
  }

  protected selectedCategoryLabels(): string {
    return this.selectedCategories()
      .map((category) => this.dayCategoryLabel(category))
      .join(', ');
  }

  protected recurrenceLabel(): string {
    return this.editForm.controls.recurrenceType.value === 'MONTHLY' ? 'Mensuelle' : 'Hebdomadaire';
  }

  protected showSeriesEditor(): boolean {
    return this.selectedScope() === 'THIS_AND_FOLLOWING' && !!this.selectedOccurrence()?.recurring;
  }

  protected canEditSeriesScope(occurrence: TaskOccurrence): boolean {
    return occurrence.recurring && !occurrence.pastLocked;
  }

  protected canDeleteOccurrence(occurrence: TaskOccurrence): boolean {
    return !occurrence.pastLocked;
  }

  protected canEditPastOccurrence(occurrence: TaskOccurrence): boolean {
    return occurrence.pastLocked;
  }

  protected pastStatusLabel(value: 'planned' | 'done' | 'missed'): string {
    switch (value) {
      case 'done':
        return 'Terminée';
      case 'missed':
        return 'Manquée';
      default:
        return 'À faire';
    }
  }

  protected seriesScopeHelperMessage(occurrence: TaskOccurrence): string {
    if (occurrence.futureScopeAvailable) {
      return '';
    }
    return "Aucune occurrence future n'existe encore, mais tu peux prolonger ou modifier la serie a partir d'ici.";
  }

  protected resultRangeLabel(): string {
    if (!this.totalElements()) {
      return '0 resultat';
    }
    const start = this.currentPage() * this.pageSize() + 1;
    const end = Math.min((this.currentPage() + 1) * this.pageSize(), this.totalElements());
    return `${start}-${end} sur ${this.totalElements()}`;
  }

  private buildSlotGroup(defaultMode: string = 'EVERY_N_MINUTES') {
    return this.fb.nonNullable.group({
      timeMode: [defaultMode, Validators.required],
      fixedTime: [''],
      intervalHours: [1],
      intervalMins: [0]
    });
  }

  private loadOccurrences(): void {
    this.loading.set(true);
    this.taskApi.listOccurrences(this.buildQuery()).subscribe({
      next: ({ data }) => {
        this.occurrences.set(data.items);
        this.currentPage.set(data.page);
        this.pageSize.set(data.size);
        this.totalElements.set(data.totalElements);
        this.totalPages.set(data.totalPages);
        this.loading.set(false);
        this.tryAutoOpenOccurrence();
      },
      error: (error) => {
        this.handleUiError(error, 'Impossible de charger les occurrences.');
        this.loading.set(false);
      }
    });
  }

  private buildQuery(): TaskOccurrenceListQuery {
    const value = this.filterForm.getRawValue();
    return {
      page: this.currentPage(),
      size: this.pageSize(),
      search: value.search.trim() || undefined,
      taskKind: value.taskKind,
      selectedDate: value.taskKind === 'RECURRING_AFTER_DATE' ? value.selectedDate || undefined : undefined,
      occurrenceDateFrom: value.occurrenceDateFrom || undefined,
      occurrenceDateTo: value.occurrenceDateTo || undefined,
      timeMode: value.timeMode,
      fixedTime: value.timeMode === 'FIXED' ? value.fixedTime || undefined : undefined,
      wakeUpOffsetMinutes:
        value.timeMode === 'WAKE_UP_OFFSET' && this.hasOffsetFilterValue(value.wakeUpOffsetHours, value.wakeUpOffsetMins)
          ? this.toWakeUpOffsetMinutes(this.toInt(value.wakeUpOffsetHours), this.toInt(value.wakeUpOffsetMins))
          : undefined
    };
  }

  private selectedCategories(): DayCategory[] {
    return DAY_CATEGORIES.filter((category) => this.editForm.controls[category].value);
  }

  private selectedDaysOfWeek(): number[] {
    return DAYS_OF_WEEK.filter((day) => {
      const key = `dow${day.value}` as const;
      return this.editForm.controls[key].value;
    }).map((day) => day.value);
  }

  private handleUiError(error: { error?: { error?: { code?: string; message?: string } } }, fallbackMessage: string): void {
    const apiError = error?.error?.error;
    const message = apiError?.message ?? fallbackMessage;
    this.errorMessage.set(message);
    this.toastMessage.set(apiError?.code === 'BUSINESS_RULE_VIOLATION' ? `Action impossible : ${message}` : '');
  }

  private onMutationCompleted(): void {
    this.successMessage.set(
      this.selectedScope() === 'THIS_OCCURRENCE'
        ? 'Occurrence mise a jour.'
        : 'Occurrence et suivantes mises a jour.'
    );
    this.toastMessage.set('');
    this.saving.set(false);
    this.closeScopeSheet();
    this.clearEditorQueryParams();
    this.loadOccurrences();
  }

  private confirmPastOccurrenceMutation(occurrence: TaskOccurrence): void {
    const newTime = this.editForm.controls.fixedTime.value || occurrence.occurrenceTime.slice(0, 5);
    const targetStatus = this.pastStatus();
    const originalStatus = this.normalizePastStatus(occurrence.status);

    const finalize = () => {
      this.successMessage.set('Occurrence passée mise à jour.');
      this.toastMessage.set('');
      this.saving.set(false);
      this.closeScopeSheet();
      this.clearEditorQueryParams();
      this.loadOccurrences();
    };

    const applyStatus = () => {
      if (targetStatus === originalStatus) {
        finalize();
        return;
      }

      const request =
        targetStatus === 'done'
          ? this.todayApi.complete(occurrence.id)
          : targetStatus === 'missed'
            ? this.todayApi.miss(occurrence.id)
            : this.todayApi.revertToPlanned(occurrence.id);

      request.subscribe({
        next: () => finalize(),
        error: (error) => {
          this.handleUiError(error, "Impossible de modifier le statut de cette occurrence passée.");
          this.saving.set(false);
        }
      });
    };

    if (newTime !== occurrence.occurrenceTime.slice(0, 5)) {
      this.todayApi.editTime(occurrence.id, newTime).subscribe({
        next: () => applyStatus(),
        error: (error) => {
          this.handleUiError(error, "Impossible de modifier l'heure de cette occurrence passée.");
          this.saving.set(false);
        }
      });
      return;
    }

    applyStatus();
  }

  private bootstrapFromQueryParams(): void {
    const params = this.route.snapshot.queryParamMap;
    this.autoOpenOccurrenceId = params.get('occurrenceId');
    this.autoOpenScope = params.get('scope') === 'THIS_AND_FOLLOWING' ? 'THIS_AND_FOLLOWING' : 'THIS_OCCURRENCE';
    this.autoOpenAction = params.get('action') === 'delete' ? 'delete' : 'edit';

    const date = params.get('date');
    if (date) {
      this.filterForm.patchValue({
        occurrenceDateFrom: date,
        occurrenceDateTo: date,
        selectedDate: date
      });
    }
    // Ensure all occurrences for the day are loaded when auto-opening
    if (this.autoOpenOccurrenceId) {
      this.pageSize.set(100);
    }
  }

  private tryAutoOpenOccurrence(): void {
    if (this.autoOpened || !this.autoOpenOccurrenceId) {
      return;
    }
    const occurrence = this.occurrences().find((item) => item.id === this.autoOpenOccurrenceId);
    if (!occurrence) {
      return;
    }
    this.autoOpened = true;
    this.openScopeSheet(this.autoOpenAction, occurrence);
    this.selectedScope.set(this.autoOpenScope);
  }

  private clearEditorQueryParams(): void {
    if (!this.route.snapshot.queryParamMap.has('occurrenceId')) {
      return;
    }
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        occurrenceId: null,
        scope: null,
        action: null,
        date: null
      },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }

  private toWakeUpOffsetMinutes(hours: number, minutes: number): number {
    return Math.min(720, Math.max(0, hours * 60 + Math.abs(minutes)));
  }

  private hasOffsetFilterValue(hours: string, minutes: string): boolean {
    return hours.trim() !== '' || minutes.trim() !== '';
  }

  private toInt(value: string): number {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private normalizePastStatus(status: string): 'planned' | 'done' | 'missed' {
    if (status === 'done' || status === 'missed') {
      return status;
    }
    return 'planned';
  }
}
