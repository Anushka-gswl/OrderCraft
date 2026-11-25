# OrderCraft — Project Status Report

Generated after full codebase inspection of:
- **Backend**: `D:\OrderCraft\Order_Management_System` (Spring Boot `4.1.1`, Java `17`, Spring Data JPA / Hibernate, MySQL `order_management_db`)
- **Frontend**: `D:\OrderCraft\order-management-frontend` (Angular `22.2.0`, TypeScript `~6.0.2`, zoneless change detection)

---

## 1. Existing Completed Modules

### Backend
- **Customer Management**
  - Entity: `Customer` (`customers`)
  - Repository / Service / Controller: `CustomerRepository`, `CustomerService`, `CustomerController` (`/api/customers`)
  - Implemented: Create (`POST`), list (`GET`), get by ID (`GET /{id}`), delete (`DELETE /{id}`), `@CrossOrigin("http://localhost:4200")`.
- **Product Management**
  - Entity: `Product` (`products`)
  - Repository / Service / Controller: `ProductRepository`, `ProductService`, `ProductController` (`/api/products`)
  - Implemented: Create (`POST`), list (`GET`), get by ID (`GET /{id}`), delete (`DELETE /{id}`), `@CrossOrigin("http://localhost:4200")`.
- **Material Management**
  - Entity: `Material` (`materials`)
  - Repository / Service / Controller: `MaterialRepository`, `MaterialService`, `MaterialController` (`/api/materials`)
  - Implemented: Create (`POST`), list (`GET`), get by ID (`GET /{id}`), delete (`DELETE /{id}`), `@CrossOrigin("http://localhost:4200")`.
- **Inventory Management**
  - Entity: `Inventory` (`inventory`)
  - Repository / Service / Controller: `InventoryRepository`, `InventoryService`, `InventoryController` (`/api/inventory`)
  - Implemented: Create (`POST`), list (`GET`), get by ID (`GET /{id}`), get by material ID (`GET /material/{materialId}`), update (`PUT /{id}`), delete (`DELETE /{id}`), `@CrossOrigin("http://localhost:4200")`.

### Frontend
- **Application Shell & Routing**: `App` (`app.ts`, `app.html`, `app.css`), `app.routes.ts`, `app.config.ts` (`provideRouter`, `provideHttpClient`).
- **Customers Page** (`/customers`): `Customers`, `CustomerService`, `Customer` interface — list, validated add form, delete.
- **Products Page** (`/products`): `Products`, `ProductService`, `Product` interface — list with active/inactive badge, validated add form, delete.
- **Materials Page** (`/materials`): `Materials`, `MaterialService`, `Material` interface — list, validated add form, delete.
- **Inventory Page** (`/inventory`): `Inventory`, `InventoryService`, `Inventory` interface — list with `Low Stock` / `Available` badge (`quantityAvailable <= reorderLevel`), add/edit form (by raw `materialId`), delete.

---

## 2. Partially Completed Modules

### Backend
- **Product–Material Mapping (BOM)** (`ProductMaterial`, `ProductMaterialRepository`, `ProductMaterialService`, `ProductMaterialController`)
  - Basic CRUD (`POST`, `GET`, `GET /{id}`, `DELETE`) exists.
  - Missing `@CrossOrigin` and missing endpoint to retrieve materials for a specific `productId` (even though `ProductMaterialRepository.findByProductProductId(Long)` exists).
- **Customer Order & Order Item Management** (`CustomerOrder`, `OrderItem`, `CustomerOrderController`, `OrderItemController`, `CustomerOrderService`, `OrderItemService`)
  - Basic CRUD (`POST`, `GET`, `GET /{id}`, `DELETE`) exists.
  - Missing `@CrossOrigin` on both controllers, missing `PUT` update endpoints, and missing `GET /api/order-items/order/{orderId}` (or nested items on order creation/retrieval).
  - `CustomerOrderService.createOrder` performs a plain `save(order)` without generating `orderNumber`, setting `orderDate`/`status`, saving line items, or calculating `totalAmount`.
- **Order Processing & Shortage / Auto-PO Logic** (`OrderProcessingService`, `CustomerOrderService.startOrderProcessing`)
  - Material requirement calculation, inventory checking, shortage detection, auto-PO creation, and `start-processing` endpoints exist.
  - Missing structured DTO responses (currently returns `List<String>`), missing material aggregation across multiple order items, missing `@Transactional` boundaries, and `startOrderProcessing` does not deduct/reserve inventory.
- **Purchase Order & Receiving** (`PurchaseOrder`, `PurchaseOrderItem`, `PurchaseOrderController`, `PurchaseOrderItemController`, `PurchaseOrderService`, `PurchaseOrderItemService`)
  - Basic CRUD and `POST /api/purchase-orders/{id}/receive` exist.
  - Missing `@CrossOrigin`, missing `GET` items by `purchaseOrderId`, missing duplicate-receive guard (can increment inventory multiple times), and missing `@Transactional` boundary.

### Frontend
- **Dashboard** (`/dashboard`): UI layout exists (`dashboard.html`), but `Dashboard` component is empty and all KPIs (`1`, `3`), Recent Order (`ORD-001`, `ABC Manufacturing`, `₹50,000`, `IN_PROGRESS`), and Quick Action buttons are hardcoded static placeholders.
- **Orders Service** (`src/app/services/order.ts`): Empty skeleton (`@Service() export class Order {}`) with no HTTP methods.
- **Orders Page** (`/orders`): Angular CLI placeholder (`<p>orders works!</p>`).
- **Purchase Orders Page** (`/purchase-orders`): Angular CLI placeholder (`<p>purchase-orders works!</p>`).

---

## 3. Missing Modules

### Backend
- **Global CORS Configuration** (`WebMvcConfigurer`) covering all `/api/**` routes.
- **Global Exception Handling & Validation** (`@RestControllerAdvice`, `spring-boot-starter-validation` not in `pom.xml`).
- **Update (`PUT`) Endpoints** for Customer, Product, Material, ProductMaterial, CustomerOrder, OrderItem, and PurchaseOrder.
- **Authentication & Role-Based Access Control**: Spring Security, password hashing, `User` / `Role` entities, login/logout/token or session auth, and role enforcement (`ADMIN`, `SALES_EXECUTIVE`, `INVENTORY_MANAGER`, `PROCUREMENT`).
- **Invoice Management**: `Invoice` / `InvoiceItem` entities, invoice generation from valid orders, duplicate-invoice prevention, PDF export.
- **Packing & Dispatch Management**: Packing/dispatch entity and status workflow (`PENDING` → `PACKED` → `READY_FOR_DISPATCH` → `DISPATCHED` → `IN_TRANSIT` → `DELIVERED`), courier/tracking/delivery address fields.
- **Supplier Management** (currently `PurchaseOrder.supplierName` is a plain string).
- **Dashboard Summary & Reporting Endpoints** (order/PO/inventory/low-stock/shortage/sales summaries).
- **Automated Tests**: Only default `contextLoads()` exists in `OrderManagementSystemApplicationTests.java`.

### Frontend
- **Models**: `CustomerOrder`, `OrderItem`, `ProductMaterial`, `PurchaseOrder`, `PurchaseOrderItem`, `User`/auth, `Invoice`, `Dispatch`.
- **Services**: `OrderService` (functional), `OrderItemService`, `ProductMaterialService`, `PurchaseOrderService`, `PurchaseOrderItemService`, `AuthService`, `InvoiceService`, `DispatchService`, `DashboardService`/`ReportService`.
- **Pages / Workflows**:
  - Product–Material BOM management UI.
  - Customer Orders management & Order Processing workflow UI.
  - Purchase Orders management & Receiving UI.
  - Login / Logout / Role-based navigation & route guards.
  - Invoice generation, preview, print, and PDF download UI.
  - Packing & Dispatch management UI.
  - Live Dashboard & Reports/Search/Export UI.
  - Edit UI for Customers, Products, and Materials.

---

## 4. Existing API Endpoints

| Controller | Base Path | Endpoints | CORS |
|---|---|---|---|
| `CustomerController` | `/api/customers` | `POST /`, `GET /`, `GET /{id}`, `DELETE /{id}` | Yes |
| `ProductController` | `/api/products` | `POST /`, `GET /`, `GET /{id}`, `DELETE /{id}` | Yes |
| `MaterialController` | `/api/materials` | `POST /`, `GET /`, `GET /{id}`, `DELETE /{id}` | Yes |
| `InventoryController` | `/api/inventory` | `POST /`, `GET /`, `GET /{id}`, `GET /material/{materialId}`, `PUT /{id}`, `DELETE /{id}` | Yes |
| `ProductMaterialController` | `/api/product-materials` | `POST /`, `GET /`, `GET /{id}`, `DELETE /{id}` | **Missing** |
| `CustomerOrderController` | `/api/orders` | `POST /`, `GET /`, `GET /{id}`, `GET /{id}/material-requirements`, `GET /{id}/inventory-check`, `GET /{id}/shortages`, `POST /{id}/create-purchase-order`, `POST /{id}/start-processing`, `DELETE /{id}` | **Missing** |
| `OrderItemController` | `/api/order-items` | `POST /`, `GET /`, `GET /{id}`, `DELETE /{id}` | **Missing** |
| `PurchaseOrderController` | `/api/purchase-orders` | `POST /`, `GET /`, `GET /{id}`, `POST /{id}/receive`, `DELETE /{id}` | **Missing** |
| `PurchaseOrderItemController` | `/api/purchase-order-items` | `POST /`, `GET /`, `GET /{id}`, `DELETE /{id}` | **Missing** |

---

## 5. Existing Database Entities & Relationships

Target schema: MySQL `order_management_db` (via `spring.jpa.hibernate.ddl-auto=update`).

| Entity | Table | Primary Key | Fields & Relationships |
|---|---|---|---|
| `Customer` | `customers` | `customerId` | `customerName`, `email`, `phone`, `address` |
| `Product` | `products` | `productId` | `productCode` (unique, not null), `productName`, `description`, `unitPrice`, `active` |
| `Material` | `materials` | `materialId` | `materialCode` (unique, not null), `materialName`, `unit`, `reorderLevel`, `unitCost` |
| `ProductMaterial` | `product_materials` | `productMaterialId` | `@ManyToOne product_id → products`, `@ManyToOne material_id → materials`, `quantityRequired`; unique `(product_id, material_id)` |
| `Inventory` | `inventory` | `inventoryId` | `@OneToOne material_id → materials` (unique, not null), `quantityAvailable` |
| `CustomerOrder` | `customer_orders` | `orderId` | `orderNumber` (unique, not null), `@ManyToOne customer_id → customers` (not null), `orderDate`, `status`, `totalAmount` |
| `OrderItem` | `order_items` | `orderItemId` | `@ManyToOne order_id → customer_orders` (not null), `@ManyToOne product_id → products` (not null), `quantity`, `unitPrice`, `subtotal` |
| `PurchaseOrder` | `purchase_orders` | `purchaseOrderId` | `poNumber` (unique, not null), `supplierName`, `orderDate`, `status`, `totalAmount` |
| `PurchaseOrderItem` | `purchase_order_items` | `purchaseOrderItemId` | `@ManyToOne purchase_order_id → purchase_orders` (not null), `@ManyToOne material_id → materials` (not null), `quantity`, `unitCost`, `subtotal` |

---

## 6. Identified Bugs, Compilation & Configuration Issues

1. **Missing CORS on 5 Controllers**: `CustomerOrderController`, `OrderItemController`, `ProductMaterialController`, `PurchaseOrderController`, and `PurchaseOrderItemController` lack `@CrossOrigin` (and no global CORS config exists), blocking Angular requests from `http://localhost:4200`.
2. **Missing `RouterLinkActive` Import (`app.ts`)**: `app.html` binds `routerLinkActive="active"` on sidebar links, while `app.ts` only imports `[RouterOutlet, RouterLink]`, so active nav styling does not apply.
3. **Outdated Unit Test (`app.spec.ts`)**: Asserts `h1` contains `'Hello, order-management-frontend'` instead of `'OrderCraft'`, and lacks `provideRouter([])` for `RouterOutlet`/`RouterLink`.
4. **Order Processing & Shortage Bugs (`OrderProcessingService`, `CustomerOrderService`)**:
   - `calculateMaterialRequirements`, `checkInventory`, `getShortages`, and `createPurchaseOrderForShortages` do not aggregate identical materials across multiple order items in the same order.
   - `createPurchaseOrderForShortages` creates an empty `PurchaseOrder` (`totalAmount = 0.0`) even when there are no shortages.
   - `startOrderProcessing` returns `allMatch(...) == true` when an order has zero items, does not validate prior order status, and does not deduct inventory.
   - `orderItemRepository.findAll()` is scanned and filtered in Java instead of using a repository query `findByOrderOrderId`.
5. **Duplicate PO Receive Bug (`PurchaseOrderService.receivePurchaseOrder`)**: Does not reject receiving a PO whose status is already `RECEIVED`, allowing duplicate inventory increments. Also lacks `@Transactional` and scans `purchaseOrderItemRepository.findAll()` in memory.
6. **Missing `PUT` Endpoints for Master Data**: `CustomerController`, `ProductController`, and `MaterialController` lack update (`PUT`) endpoints even though Customer/Product/Material management expects full CRUD.
7. **Inventory Form Sends Partial Nested `Material` (`inventory.ts`)**: Creating/updating inventory passes `{ material: { materialId, ... }, quantityAvailable }` with empty fields for `materialCode`/`materialName` on create.
8. **Plaintext DB Credentials & Missing Validation**: `application.properties` stores plaintext MySQL credentials (`root` / `Pass@123@word`); `pom.xml` lacks `spring-boot-starter-validation` and exception handling returns generic 500s on `RuntimeException`.
9. **Baseline Verification Notes**:
   - Frontend `tsc --noEmit -p tsconfig.app.json` and `ngc.cmd --noEmit -p tsconfig.app.json` both passed with exit code `0`.
   - Backend Maven compilation and `mysql` CLI inspection could not complete inside the sandbox (`java.exe` stalled in the sandboxed environment and `mysql` CLI is not on `PATH`); unsandboxed execution will be needed for Maven builds/tests and DB checks.

---

## 7. Proposed Implementation Sequence

Following controlled phase-by-phase development (each phase requires explicit approval before starting the next):

1. **Phase 1 — Customer Order Management (Module A)** *(Next)*
   - **Proposed Backend Changes (no DB schema changes)**:
     - Add global CORS configuration (`WebMvcConfigurer` for `http://localhost:4200`) and `@RestControllerAdvice` global exception handler.
     - Add `findByOrderOrderId(Long orderId)` to `OrderItemRepository` and `GET /api/order-items/order/{orderId}`.
     - Enhance `CustomerOrderService` / `CustomerOrderController`: support creating an order with items (auto-generating `orderNumber` if omitted, setting `orderDate`, default status `NEW`, computing item `unitPrice`/`subtotal` from `Product` and `totalAmount`), updating editable (`NEW`) orders (`PUT /api/orders/{id}`), and adding missing `PUT` endpoints on Customers/Products/Materials if needed for completeness while preserving all existing endpoints/contracts.
     - Also fix `RouterLinkActive` import in `app.ts`.
   - **Proposed Frontend Changes**:
     - Create `CustomerOrder` and `OrderItem` models, implement `OrderService` (`services/order.ts`) and `OrderItemService`.
     - Build the full `/orders` page: searchable/filterable orders table, status badges, order creation/edit form with customer selector, multi-product line-item builder (auto-fetching product prices, computing subtotals & total), order details modal/drawer, and validated delete.
2. **Phase 2 — Purchase Order Management (Module B)**
   - Backend: `findByPurchaseOrderPurchaseOrderId`, PO items endpoint, duplicate-receive guard & `@Transactional` on `receivePurchaseOrder`, manual PO creation with items, `PUT`/status validation.
   - Frontend: `PurchaseOrder`/`PurchaseOrderItem` models & services, full `/purchase-orders` page with details, manual PO creation, receive PO action + confirmation + inventory state update.
3. **Phase 3 — Order Processing Workflow & Product BOM (Module C)**
   - Backend: Aggregate duplicate materials in `OrderProcessingService`, prevent empty shortage PO creation, structured DTOs alongside existing string endpoints (preserving existing contracts), stock deduction/reservation and valid status transitions (`NEW` → `IN_PROGRESS` → `COMPLETED` / `CANCELLED`), `GET /api/product-materials/product/{productId}`.
   - Frontend: BOM management UI on `/products` and visual step-by-step order processing workflow on `/orders` (requirements → inventory check → shortages → auto-PO → recheck → start/complete processing).
4. **Phase 4 — Authentication & Role-Based Access Control (Module D)**
   - Backend: `User` & `Role` entities, Spring Security + BCrypt password hashing + JWT/stateless auth (configurable secret via env/properties, no hardcoded secrets), role-based endpoint protection (`ADMIN`, `SALES_EXECUTIVE`, `INVENTORY_MANAGER`, `PROCUREMENT`), 401/403 handlers.
   - Frontend: Login page, `AuthService`, HTTP auth interceptor, route guards, role-based sidebar navigation, user management UI (Admin).
5. **Phase 5 — Invoice Management (Module E)**
   - Backend: `Invoice` & `InvoiceItem` entities, unique invoice number generation from valid orders, duplicate-invoice prevention, payment status updates.
   - Frontend: Invoice page/modal with OrderCraft branding, itemized table, print support, and PDF download.
6. **Phase 6 — Packing & Dispatch Management (Module F)**
   - Backend: `Dispatch` / packing tracking entity linked to `CustomerOrder`, validated status transitions (`PENDING` → `PACKED` → `READY_FOR_DISPATCH` → `DISPATCHED` → `IN_TRANSIT` → `DELIVERED`), dates, courier, tracking number, delivery address.
   - Frontend: Packing & Dispatch management page integrated with order status.
7. **Phase 7 — Dashboard Improvements & Reporting/Search (Modules G & H)**
   - Backend: Aggregated dashboard metrics & reporting endpoints (customer orders, POs, inventory, low-stock, shortages, sales/status summaries).
   - Frontend: Live data-driven Dashboard with real charts/alerts & working Quick Actions; Reports page with search, sorting, filtering, pagination, print/CSV export.
8. **Phase 8 — Integration Testing & Bug Fixes**
   - End-to-end verification across backend APIs, MySQL `order_management_db`, and Angular UI, plus unit/integration tests.
