# Spring Kotlin Orkes Conductor

This project demonstrates Spring Boot with Orkes Conductor using a real-world order fulfillment scenario.

The workflow validates an order, reserves inventory, authorizes payment, branches on the payment result, schedules delivery for approved payments, releases inventory for declined payments, and sends the customer a notification.

## Setup Server

Docker Compose file to run Conductor server is provided in `docker-compose.yml`. Start it with:

```sh
docker-compose up -d
```

The Conductor API is expected at `http://localhost:9090/api/`.

## Scenario

- Workflow: `order_fulfillment_workflow`
- Task: `validate_order`
- Task: `reserve_inventory`
- Task: `process_payment`
- Decision: `payment_decision`
- Approved branch: `schedule_delivery` -> `send_order_notification`
- Declined/default branch: `release_inventory` -> `send_order_notification`

## Run

Start the Spring app, then trigger the workflow:

```http
POST http://localhost:8081/api/v1/orders
Content-Type: application/json

{
  "orderId": "ORD-1001",
  "customerId": "CUS-42",
  "items": [
    {
      "sku": "BOOK-001",
      "quantity": 1,
      "price": 24.99
    }
  ],
  "totalAmount": 24.99,
  "deliveryAddress": "Levent, Istanbul",
  "paymentMethod": "CARD",
  "priority": "EXPRESS",
  "notificationChannel": "EMAIL"
}
```

Set `"paymentMethod": "DECLINED"` to test the compensation path. The workflow will release the inventory reservation and send a payment-declined notification.

## Workflow Metadata

Task definitions live in `src/main/resources/task.json`.

Workflow definition lives in `src/main/resources/workflow.json`.

The app loads both into Conductor on startup.

## Configuration

```yaml
conductor:
  url: http://localhost:9090/api/
  threadCount: 5
  timeOut: 10_000
```
