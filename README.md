# Pet Clinic Backend

## Inventory Management (Batch Wise)

This backend now supports pharmacy inventory management using `stock_batch` records.

### Implemented Features

- Add medicine stock per batch (no supplier information stored)
- View medicine inventory totals
- View batch list per medicine
- Sell medicine with automatic FEFO deduction (earliest-expiry-first)
- Prevent negative stock by validating available quantity inside a transaction

### API Endpoints

- `GET /api/inventory` - list all medicines with total available quantity
- `GET /api/inventory/{medicineId}/batches` - list batches for one medicine
- `POST /api/inventory/batches` - add a new stock batch
- `POST /api/inventory/sales` - sell medicine and reduce inventory

### Example Requests

`POST /api/inventory/batches`

```json
{
  "medicineId": 7,
  "batchNo": "B-2026-04",
  "expiryDate": "2027-04-30",
  "purchasePrice": 42.50,
  "quantity": 100,
  "receivedAt": "2026-04-19T10:30:00"
}
```

`POST /api/inventory/sales`

```json
{
  "medicineId": 7,
  "quantity": 12
}
```

### Running Tests

```powershell
.\mvnw.cmd -Dtest=InventoryServiceTest test
```

