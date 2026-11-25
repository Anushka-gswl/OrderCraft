import { Routes } from '@angular/router';

import { Login } from './pages/login/login';
import { Dashboard } from './pages/dashboard/dashboard';
import { Customers } from './pages/customers/customers';
import { Products } from './pages/products/products';
import { Materials } from './pages/materials/materials';
import { Inventory } from './pages/inventory/inventory';
import { Orders } from './pages/orders/orders';
import { PurchaseOrders } from './pages/purchase-orders/purchase-orders';
import { Invoices } from './pages/invoices/invoices';
import { Users } from './pages/users/users';
import { authGuard, guestOnlyGuard, roleGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    component: Login,
    canActivate: [guestOnlyGuard],
  },
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'dashboard',
    component: Dashboard,
    canActivate: [authGuard],
  },
  {
    path: 'customers',
    component: Customers,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN', 'SALES_EXECUTIVE'] },
  },
  {
    path: 'products',
    component: Products,
    canActivate: [authGuard, roleGuard],
    data: {
      roles: ['ADMIN', 'SALES_EXECUTIVE', 'INVENTORY_MANAGER', 'PROCUREMENT'],
    },
  },
  {
    path: 'materials',
    component: Materials,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN', 'INVENTORY_MANAGER', 'PROCUREMENT'] },
  },
  {
    path: 'inventory',
    component: Inventory,
    canActivate: [authGuard, roleGuard],
    data: {
      roles: ['ADMIN', 'INVENTORY_MANAGER', 'PROCUREMENT', 'SALES_EXECUTIVE'],
    },
  },
  {
    path: 'orders',
    component: Orders,
    canActivate: [authGuard, roleGuard],
    data: {
      roles: ['ADMIN', 'SALES_EXECUTIVE', 'INVENTORY_MANAGER', 'PROCUREMENT'],
    },
  },
  {
    path: 'purchase-orders',
    component: PurchaseOrders,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN', 'PROCUREMENT', 'INVENTORY_MANAGER'] },
  },
  {
    path: 'invoices',
    component: Invoices,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN', 'SALES_EXECUTIVE'] },
  },
  {
    path: 'users',
    component: Users,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
