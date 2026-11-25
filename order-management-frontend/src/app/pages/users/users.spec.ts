import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Users } from './users';
import { AuthService } from '../../services/auth';

describe('Users Component', () => {
  let component: Users;
  let fixture: ComponentFixture<Users>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Users],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: {
            currentUser: () => ({
              userId: 1,
              username: 'admin',
              fullName: 'Admin User',
              email: 'admin@test.local',
              role: 'ADMIN',
              active: true,
            }),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Users);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load users on init', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne('http://localhost:8080/api/users');
    req.flush([
      {
        userId: 1,
        username: 'admin',
        fullName: 'Admin User',
        email: 'admin@test.local',
        role: 'ADMIN',
        active: true,
      },
    ]);

    expect(component.users.length).toBe(1);
    expect(component.isCurrentUser(component.users[0])).toBe(true);
  });

  it('should validate username and password on create', () => {
    fixture.detectChanges();
    httpMock.expectOne('http://localhost:8080/api/users').flush([]);

    component.openCreateUserForm();
    component.userForm.fullName = 'Jane Doe';
    component.userForm.email = 'jane@test.local';
    component.userForm.username = 'ja';
    component.saveUser();

    expect(component.formErrorMessage).toContain('at least 3 characters');
  });

  it('should create a new user', () => {
    fixture.detectChanges();
    httpMock.expectOne('http://localhost:8080/api/users').flush([]);

    component.openCreateUserForm();
    component.userForm.fullName = 'Jane Doe';
    component.userForm.email = 'jane@test.local';
    component.userForm.username = 'janedoe';
    component.userForm.password = 'secret123';
    component.userForm.role = 'SALES_EXECUTIVE';
    component.saveUser();

    const postReq = httpMock.expectOne('http://localhost:8080/api/users');
    expect(postReq.request.method).toBe('POST');
    postReq.flush({
      userId: 2,
      username: 'janedoe',
      fullName: 'Jane Doe',
      email: 'jane@test.local',
      role: 'SALES_EXECUTIVE',
      active: true,
    });

    httpMock.expectOne('http://localhost:8080/api/users').flush([]);

    expect(component.successMessage).toContain('janedoe');
    expect(component.showForm).toBe(false);
  });
});
