import { Component, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TaskApiService, type TaskPreviewResponse } from './task-api.service';
import { EmojiPickerComponent } from './emoji-picker.component';
import { StepFormComponent } from '../../shared/components/step-form.component';

const DAY_CATEGORIES = ['WORKDAY', 'VACATION', 'WEEKEND_HOLIDAY'] as const;
type DayCategory = (typeof DAY_CATEGORIES)[number];

const DAY_CATEGORY_LABELS: Record<DayCategory, string> = {
  WORKDAY: 'Travail',
  VACATION: 'Vacances',
  WEEKEND_HOLIDAY: 'Week-end et férié'
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
  selector: 'app-task-create-page',
  imports: [ReactiveFormsModule, RouterLink, EmojiPickerComponent, StepFormComponent],
  templateUrl: './task-create-page.component.html',
  styleUrl: './task-create-page.component.scss'
})
export class TaskCreatePageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly taskApi = inject(TaskApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly currentStep = signal(1);
  protected readonly totalSteps = 5;
  protected readonly submitting = signal(false);
  protected readonly previewing = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly preview = signal<TaskPreviewResponse | null>(null);
  protected readonly selectedPhotoName = signal('');
  protected readonly showEmojiPicker = signal(false);

  protected readonly dayCategoryLabels = DAY_CATEGORY_LABELS;
  protected readonly dayCategoryKeys = DAY_CATEGORIES;
  protected readonly daysOfWeek = DAYS_OF_WEEK;

  protected readonly today = new Date().toISOString().split('T')[0];

  private taskPhotoFile: File | null = null;

  protected onEmojiSelected(emoji: string): void {
    this.infoForm.controls.icon.setValue(emoji);
    this.showEmojiPicker.set(false);
  }

  protected readonly infoForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(100)]],
    icon: ['', [Validators.required, Validators.maxLength(50)]],
    description: ['']
  });

  protected readonly planningForm = this.fb.nonNullable.group({
    WORKDAY: [false],
    VACATION: [false],
    WEEKEND_HOLIDAY: [false],
    startDate: ['', Validators.required],
    recurrenceType: ['NONE'],
    dow1: [false], dow2: [false], dow3: [false], dow4: [false],
    dow5: [false], dow6: [false], dow7: [false],
    dayOfMonth: [1],
    endDate: [''],
    endTime: ['']
  });

  protected readonly scheduleForm = this.fb.nonNullable.group({
    timeMode: ['FIXED', Validators.required],
    fixedTime: ['08:00'],
    wakeUpOffsetHours: [0],
    wakeUpOffsetMins: [30]
  });

  constructor() {
    const requestedDate = this.route.snapshot.queryParamMap.get('date');
    if (requestedDate) {
      this.planningForm.patchValue({ startDate: requestedDate });
    }
  }

  protected readonly timeSlotsArray = this.fb.array<ReturnType<typeof this.buildSlotGroup>>([]);

  protected addTimeSlot(): void {
    this.timeSlotsArray.push(this.buildSlotGroup('EVERY_N_MINUTES'));
  }

  protected removeTimeSlot(index: number): void {
    this.timeSlotsArray.removeAt(index);
  }

  private buildSlotGroup(defaultMode: string = 'EVERY_N_MINUTES') {
    return this.fb.nonNullable.group({
      timeMode: [defaultMode, Validators.required],
      fixedTime: [''],
      intervalHours: [1],
      intervalMins: [0]
    });
  }

  protected get slots(): FormArray {
    return this.timeSlotsArray;
  }

  protected slotScheduleSummary(slot: ReturnType<typeof this.buildSlotGroup>): string {
    const mode = slot.controls.timeMode.value;
    if (mode === 'EVERY_N_MINUTES') {
      const h = slot.controls.intervalHours.value;
      const m = slot.controls.intervalMins.value;
      return h > 0 ? `Toutes les ${h}h${m > 0 ? m + 'min' : ''}` : `Toutes les ${m} min`;
    }
    return `Heure fixe : ${slot.controls.fixedTime.value}`;
  }

  protected infoControlInvalid(controlName: keyof typeof this.infoForm.controls): boolean {
    const control = this.infoForm.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  protected planningControlInvalid(controlName: keyof typeof this.planningForm.controls): boolean {
    const control = this.planningForm.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  protected selectedCategories(): DayCategory[] {
    return DAY_CATEGORIES.filter((key) => this.planningForm.controls[key].value);
  }

  protected selectedCategoryLabels(): string {
    return this.selectedCategories()
      .map((category) => this.dayCategoryLabels[category])
      .join(', ');
  }

  protected selectedDaysOfWeek(): number[] {
    return DAYS_OF_WEEK.filter((d) => {
      const key = `dow${d.value}` as keyof typeof this.planningForm.controls;
      return this.planningForm.controls[key].value;
    }).map((d) => d.value);
  }

  protected onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0) ?? null;
    this.taskPhotoFile = file;
    this.selectedPhotoName.set(file?.name ?? '');
  }

  protected goToStep(step: number): void {
    this.errorMessage.set('');
    this.currentStep.set(step);
  }

  protected nextStep(): void {
    this.errorMessage.set('');
    const step = this.currentStep();

    if (step === 1) {
      if (this.infoForm.invalid) {
        this.infoForm.markAllAsTouched();
        return;
      }
    }

    if (step === 3) {
      if (this.selectedCategories().length === 0) {
        this.errorMessage.set('Sélectionne au moins une catégorie de jour.');
        return;
      }
      const startDate = this.planningForm.controls.startDate;
      if (startDate.invalid) {
        startDate.markAsTouched();
        return;
      }
      if (startDate.value < this.today) {
        this.errorMessage.set('La date de début ne peut pas être dans le passé.');
        return;
      }
      const recurrenceType = this.planningForm.controls.recurrenceType.value;
      if (recurrenceType === 'WEEKLY' && this.selectedDaysOfWeek().length === 0) {
        this.errorMessage.set('Sélectionne au moins un jour de la semaine.');
        return;
      }
    }

    if (step === 4) {
      const timeMode = this.scheduleForm.controls.timeMode.value;
      if (timeMode === 'FIXED' && !this.scheduleForm.controls.fixedTime.value) {
        this.errorMessage.set('Heure fixe requise.');
        return;
      }
      this.loadPreview();
      return;
    }

    this.currentStep.set(step + 1);
  }

  protected prevStep(): void {
    this.errorMessage.set('');
    this.currentStep.update((step) => Math.max(1, step - 1));
  }

  protected loadPreview(): void {
    this.previewing.set(true);
    this.errorMessage.set('');
    this.preview.set(null);

    const planning = this.planningForm.getRawValue();
    const schedule = this.scheduleForm.getRawValue();
    const recurrenceType = planning.recurrenceType === 'NONE' ? null : planning.recurrenceType;

    const request = {
      startDate: planning.startDate,
      dayCategories: this.selectedCategories() as string[],
      timeMode: schedule.timeMode,
      fixedTime: schedule.timeMode === 'FIXED' ? schedule.fixedTime : null,
      wakeUpOffsetMinutes: schedule.timeMode === 'WAKE_UP_OFFSET' ? this.toWakeUpOffsetMinutes(schedule.wakeUpOffsetHours, schedule.wakeUpOffsetMins) : null,
      recurrenceType,
      daysOfWeek: recurrenceType === 'WEEKLY' ? this.selectedDaysOfWeek() : null,
      dayOfMonth: recurrenceType === 'MONTHLY' ? planning.dayOfMonth : null
    };

    this.taskApi.previewNextOccurrence(request).subscribe({
      next: ({ data }) => {
        this.preview.set(data);
        this.previewing.set(false);
        this.currentStep.set(5);
      },
      error: (error) => {
        this.errorMessage.set(error?.error?.error?.message ?? 'Impossible de calculer la prochaine occurrence.');
        this.previewing.set(false);
      }
    });
  }

  protected submit(): void {
    if (this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set('');

    const info = this.infoForm.getRawValue();
    const planning = this.planningForm.getRawValue();
    const schedule = this.scheduleForm.getRawValue();
    const categories = this.selectedCategories();
    const recurrenceType = planning.recurrenceType === 'NONE' ? null : planning.recurrenceType;

    const payload = new FormData();
    payload.set('title', info.title.trim());
    payload.set('icon', info.icon.trim());
    if (info.description.trim()) {
      payload.set('description', info.description.trim());
    }
    payload.set('startDate', planning.startDate);
    categories.forEach((cat, index) => payload.set(`dayCategories[${index}]`, cat));
    payload.set('timeMode', schedule.timeMode);
    if (schedule.timeMode === 'FIXED') {
      payload.set('fixedTime', schedule.fixedTime);
    } else {
      payload.set('wakeUpOffsetMinutes', String(this.toWakeUpOffsetMinutes(schedule.wakeUpOffsetHours, schedule.wakeUpOffsetMins)));
    }
    if (recurrenceType) {
      payload.set('recurrenceType', recurrenceType);
      if (recurrenceType === 'WEEKLY') {
        this.selectedDaysOfWeek().forEach((d, i) => payload.set(`daysOfWeek[${i}]`, String(d)));
      }
      if (recurrenceType === 'MONTHLY') {
        payload.set('dayOfMonth', String(planning.dayOfMonth));
      }
      if (planning.endDate) {
        payload.set('endDate', planning.endDate);
      }
    }
    if (planning.endTime) {
      payload.set('endTime', planning.endTime);
    }
    if (this.taskPhotoFile) {
      payload.set('photo', this.taskPhotoFile);
    }

    const slots = this.timeSlotsArray.getRawValue();
    slots.forEach((slot, i) => {
      payload.set(`timeSlots[${i}].timeMode`, slot.timeMode);
      if (slot.timeMode === 'FIXED' && slot.fixedTime) {
        payload.set(`timeSlots[${i}].fixedTime`, slot.fixedTime);
      } else if (slot.timeMode === 'EVERY_N_MINUTES') {
        const totalMinutes = Math.min(720, Math.max(1, slot.intervalHours * 60 + slot.intervalMins));
        payload.set(`timeSlots[${i}].afterPreviousMinutes`, String(totalMinutes));
      }
    });

    this.taskApi.createTask(payload).subscribe({
      next: async () => {
        this.submitting.set(false);
        await this.router.navigateByUrl('/tasks/manage');
      },
      error: (error) => {
        this.errorMessage.set(error?.error?.error?.message ?? 'Impossible de créer la tâche.');
        this.submitting.set(false);
        this.currentStep.set(1);
      }
    });
  }

  protected recurrenceSummary(): string {
    const type = this.planningForm.controls.recurrenceType.value;
    if (type === 'WEEKLY') {
      const days = this.selectedDaysOfWeek().map((d) => DAYS_OF_WEEK.find((x) => x.value === d)?.label ?? '').join(', ');
      return `Hebdomadaire : ${days}`;
    }
    if (type === 'MONTHLY') {
      return `Mensuelle : le ${this.planningForm.controls.dayOfMonth.value} du mois`;
    }
    return 'Ponctuelle';
  }

  private toWakeUpOffsetMinutes(hours: number, minutes: number): number {
    return Math.min(720, Math.max(0, hours * 60 + Math.abs(minutes)));
  }
}
