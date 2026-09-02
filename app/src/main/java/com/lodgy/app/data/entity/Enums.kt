package com.lodgy.app.data.entity

enum class RoomType { SINGLE, DOUBLE, TRIPLE }

enum class BedStatus { VACANT, OCCUPIED }

enum class TenantStatus { ACTIVE, VACATED }

enum class AgreementStatus { ACTIVE, CLOSED }

enum class InvoiceStatus { UNPAID, PARTIAL, PAID }

enum class PaymentMode { CASH, UPI, BANK_TRANSFER, OTHER }

enum class NoteType { COMPLAINT, DAMAGE, GENERAL }

enum class ExpenseCategory { WIFI, WATER, ELECTRICITY, TAX, MAINTENANCE, REPAIR, OTHER }
