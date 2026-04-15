import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss'
})
export class LoginPageComponent {
  private static readonly loginUrl = '/api/v1/auth/login';
  private static readonly jsonHeaders = new HttpHeaders({
    'Content-Type': 'application/json',
    Accept: 'application/json'
  });

  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  protected readonly submitting = signal(false);
  protected readonly successMessage = signal('');
  protected readonly errorMessage = signal('');

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.http
      .post<{ data: { accessToken: string; expiresInSeconds: number } }>(
        LoginPageComponent.loginUrl,
        this.form.getRawValue(),
        { headers: LoginPageComponent.jsonHeaders, withCredentials: true }
      )
      .subscribe({
        next: async ({ data }) => {
          this.authService.setAccessToken(data.accessToken);
          this.submitting.set(false);
          await this.router.navigateByUrl('/today');
        },
        error: (error) => {
          this.errorMessage.set(error?.error?.error?.message ?? 'Impossible de se connecter.');
          this.submitting.set(false);
        }
      });
  }
}
