package com.yourteam.finwise.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.yourteam.finwise.data.entities.Category
import com.yourteam.finwise.data.entities.Transaction
import com.yourteam.finwise.data.entities.Debt
import com.yourteam.finwise.data.entities.User
import at.favre.lib.crypto.bcrypt.BCrypt
import com.yourteam.finwise.utils.SecurityUtils

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "finwise.db"
        private const val DATABASE_VERSION = 3

        // Table names
        const val TABLE_USERS = "users"
        const val TABLE_CATEGORIES = "categories"
        const val TABLE_TRANSACTIONS = "transactions"
        const val TABLE_DEBTS = "debts"

        // Common columns
        const val COLUMN_ID = "id"
        const val COLUMN_CREATED_AT = "created_at"

        // Users table
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD = "password"

        // Categories table
        const val COLUMN_NAME = "name"
        const val COLUMN_TYPE = "type"
        const val COLUMN_COLOR = "color"
        const val COLUMN_ICON = "icon"

        // Transactions table
        const val COLUMN_AMOUNT = "amount"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_DATE = "date"
        const val COLUMN_CATEGORY_ID = "category_id"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_TRANSACTION_TYPE = "type"

        // Debts table
        const val COLUMN_PERSON_NAME = "person_name"
        const val COLUMN_DEBT_TYPE = "debt_type"
        const val COLUMN_DUE_DATE = "due_date"
        const val COLUMN_PAY_DATE = "pay_date"
        const val COLUMN_IS_PAID = "is_paid"
    }

    private val appContext: Context = context.applicationContext

    // Safe cursor extension functions
    private fun android.database.Cursor.getColumnIndexOrNull(columnName: String): Int? {
        val index = getColumnIndex(columnName)
        return if (index >= 0) index else null
    }

    private fun android.database.Cursor.getStringOrNull(columnName: String): String? {
        return getColumnIndexOrNull(columnName)?.let { getString(it) }
    }

    private fun android.database.Cursor.getLongOrNull(columnName: String): Long? {
        return getColumnIndexOrNull(columnName)?.let { getLong(it) }
    }

    private fun android.database.Cursor.getDoubleOrNull(columnName: String): Double? {
        return getColumnIndexOrNull(columnName)?.let { getDouble(it) }
    }

    private fun android.database.Cursor.getIntOrNull(columnName: String): Int? {
        return getColumnIndexOrNull(columnName)?.let { getInt(it) }
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d("DatabaseHelper", "Creating database tables...")

        // Create users table
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_EMAIL TEXT UNIQUE NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()

        // Create categories table
        val createCategoriesTable = """
            CREATE TABLE $TABLE_CATEGORIES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_COLOR TEXT,
                $COLUMN_ICON TEXT,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()

        // Create transactions table
        val createTransactionsTable = """
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID INTEGER NOT NULL,
                $COLUMN_AMOUNT REAL NOT NULL,
                $COLUMN_TRANSACTION_TYPE TEXT NOT NULL,
                $COLUMN_CATEGORY_ID INTEGER NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_DATE INTEGER NOT NULL,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_USERS($COLUMN_ID),
                FOREIGN KEY ($COLUMN_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COLUMN_ID)
            )
        """.trimIndent()

        // Create debts table
        val createDebtsTable = """
            CREATE TABLE $TABLE_DEBTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID INTEGER NOT NULL,
                $COLUMN_PERSON_NAME TEXT NOT NULL,
                $COLUMN_AMOUNT REAL NOT NULL,
                $COLUMN_DEBT_TYPE TEXT NOT NULL,
                $COLUMN_DUE_DATE INTEGER NOT NULL,
                $COLUMN_PAY_DATE INTEGER,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_IS_PAID INTEGER DEFAULT 0,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_USERS($COLUMN_ID)
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createCategoriesTable)
        db.execSQL(createTransactionsTable)
        db.execSQL(createDebtsTable)

        // Insert default categories
        insertDefaultCategories(db)

        Log.d("DatabaseHelper", "Database tables created successfully!")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("DatabaseHelper", "Upgrading database from version $oldVersion to $newVersion")

        try {
            when (oldVersion) {
                1 -> {
                    upgradeToVersion2(db)
                }
                2 -> {
                    upgradeToVersion3(db)
                }
            }
            Log.d("DatabaseHelper", "Database upgrade completed successfully!")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error during database upgrade: ${e.message}", e)
            recreateDatabase(db)
        }
    }

    private fun upgradeToVersion2(db: SQLiteDatabase) {
        val createDebtsTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_DEBTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID INTEGER NOT NULL,
                $COLUMN_PERSON_NAME TEXT NOT NULL,
                $COLUMN_AMOUNT REAL NOT NULL,
                $COLUMN_DEBT_TYPE TEXT NOT NULL,
                $COLUMN_DUE_DATE INTEGER NOT NULL,
                $COLUMN_PAY_DATE INTEGER,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_IS_PAID INTEGER DEFAULT 0,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_USERS($COLUMN_ID)
            )
        """.trimIndent()
        db.execSQL(createDebtsTable)
        Log.d("DatabaseHelper", "Added debts table for migration from v1 to v2")
    }

    private fun upgradeToVersion3(db: SQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE $TABLE_DEBTS ADD COLUMN $COLUMN_PAY_DATE INTEGER")
            Log.d("DatabaseHelper", "Added pay_date column to debts table")
        } catch (e: Exception) {
            Log.d("DatabaseHelper", "pay_date column already exists or couldn't be added: ${e.message}")
        }
    }

    private fun recreateDatabase(db: SQLiteDatabase) {
        Log.w("DatabaseHelper", "Recreating database due to upgrade failure")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DEBTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSACTIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // ==================== USER MANAGEMENT METHODS ====================

    fun addUser(user: User): Long {
        val db = writableDatabase
        val hashedPassword = BCrypt.withDefaults().hashToString(12, user.password.toCharArray())

        val values = ContentValues().apply {
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_PASSWORD, hashedPassword)
            put(COLUMN_CREATED_AT, System.currentTimeMillis())
        }
        return try {
            db.insert(TABLE_USERS, null, values)
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error adding user: ${e.message}")
            -1L
        }
    }

    fun doesUserExist(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID),
            "$COLUMN_EMAIL = ?",
            arrayOf(email),
            null, null, null
        )
        return cursor.use { it.count > 0 }
    }

    fun authenticateUser(email: String, password: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COLUMN_EMAIL = ?",
            arrayOf(email),
            null, null, null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                val storedHash = it.getStringOrNull(COLUMN_PASSWORD)
                if (storedHash != null) {
                    val result = BCrypt.verifyer().verify(password.toCharArray(), storedHash.toCharArray())
                    if (result.verified) {
                        User(
                            id = it.getLongOrNull(COLUMN_ID) ?: 0L,
                            email = it.getStringOrNull(COLUMN_EMAIL) ?: "",
                            password = storedHash,
                            createdAt = it.getLongOrNull(COLUMN_CREATED_AT) ?: 0L
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    fun getCurrentUserId(): Long {
        val sharedPreferences = SecurityUtils.getEncryptedPreferences(appContext)
        return sharedPreferences.getLong("current_user_id", -1)
    }

    // ==================== CATEGORY METHODS ====================

    private fun insertDefaultCategories(db: SQLiteDatabase) {
        val defaultCategories = listOf(
            Category(name = "Salary", type = "INCOME", color = "#4CAF50"),
            Category(name = "Freelance", type = "INCOME", color = "#4CAF50"),
            Category(name = "Food & Dining", type = "EXPENSE", color = "#FF9800"),
            Category(name = "Transportation", type = "EXPENSE", color = "#2196F3"),
            Category(name = "Shopping", type = "EXPENSE", color = "#E91E63"),
            Category(name = "Entertainment", type = "EXPENSE", color = "#9C27B0"),
            Category(name = "Utilities", type = "EXPENSE", color = "#FF5722"),
            Category(name = "Healthcare", type = "EXPENSE", color = "#F44336")
        )

        defaultCategories.forEach { category ->
            val values = ContentValues().apply {
                put(COLUMN_NAME, category.name)
                put(COLUMN_TYPE, category.type)
                put(COLUMN_COLOR, category.color)
                put(COLUMN_ICON, category.icon ?: "")
                put(COLUMN_CREATED_AT, System.currentTimeMillis())
            }
            db.insert(TABLE_CATEGORIES, null, values)
        }
    }

    fun getAllCategories(): List<Category> {
        val categories = mutableListOf<Category>()
        val db = readableDatabase
        val cursor = db.query(TABLE_CATEGORIES, null, null, null, null, null, null)

        cursor.use {
            while (it.moveToNext()) {
                val category = Category(
                    id = it.getLongOrNull(COLUMN_ID) ?: 0L,
                    name = it.getStringOrNull(COLUMN_NAME) ?: "",
                    type = it.getStringOrNull(COLUMN_TYPE) ?: "",
                    color = it.getStringOrNull(COLUMN_COLOR) ?: "",
                    icon = it.getStringOrNull(COLUMN_ICON) ?: "",
                    createdAt = it.getLongOrNull(COLUMN_CREATED_AT) ?: 0L
                )
                categories.add(category)
            }
        }
        return categories
    }

    // ==================== TRANSACTION METHODS ====================

    fun addTransaction(transaction: Transaction): Long {
        val db = writableDatabase
        val userId = getCurrentUserId()
        if (userId == -1L) {
            Log.e("DatabaseHelper", "No user logged in when adding transaction")
            return -1L
        }

        return try {
            val values = ContentValues().apply {
                put(COLUMN_USER_ID, userId)
                put(COLUMN_AMOUNT, transaction.amount)
                put(COLUMN_TRANSACTION_TYPE, transaction.type)
                put(COLUMN_CATEGORY_ID, transaction.categoryId)
                put(COLUMN_DESCRIPTION, transaction.description ?: "")
                put(COLUMN_DATE, transaction.date)
                put(COLUMN_CREATED_AT, System.currentTimeMillis())
            }
            val result = db.insert(TABLE_TRANSACTIONS, null, values)
            Log.d("DatabaseHelper", "Transaction inserted with ID: $result")
            result
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error adding transaction: ${e.message}")
            -1L
        }
    }

    fun getAllTransactions(): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val userId = getCurrentUserId()
        if (userId == -1L) {
            Log.e("DatabaseHelper", "No user logged in when fetching transactions")
            return transactions
        }

        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COLUMN_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null,
            "$COLUMN_CREATED_AT DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val transaction = Transaction(
                    id = it.getLongOrNull(COLUMN_ID) ?: 0L,
                    amount = it.getDoubleOrNull(COLUMN_AMOUNT) ?: 0.0,
                    type = it.getStringOrNull(COLUMN_TRANSACTION_TYPE) ?: "",
                    categoryId = it.getLongOrNull(COLUMN_CATEGORY_ID) ?: 0L,
                    description = it.getStringOrNull(COLUMN_DESCRIPTION) ?: "",
                    date = it.getLongOrNull(COLUMN_DATE) ?: 0L,
                    createdAt = it.getLongOrNull(COLUMN_CREATED_AT) ?: 0L
                )
                transactions.add(transaction)
            }
        }
        Log.d("DatabaseHelper", "Loaded ${transactions.size} transactions for user $userId")
        return transactions
    }

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val userId = getCurrentUserId()
        if (userId == -1L) return transactions

        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COLUMN_USER_ID = ? AND $COLUMN_DATE BETWEEN ? AND ?",
            arrayOf(userId.toString(), startDate.toString(), endDate.toString()),
            null, null,
            "$COLUMN_DATE DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val transaction = Transaction(
                    id = it.getLongOrNull(COLUMN_ID) ?: 0L,
                    amount = it.getDoubleOrNull(COLUMN_AMOUNT) ?: 0.0,
                    type = it.getStringOrNull(COLUMN_TRANSACTION_TYPE) ?: "",
                    categoryId = it.getLongOrNull(COLUMN_CATEGORY_ID) ?: 0L,
                    description = it.getStringOrNull(COLUMN_DESCRIPTION) ?: "",
                    date = it.getLongOrNull(COLUMN_DATE) ?: 0L,
                    createdAt = it.getLongOrNull(COLUMN_CREATED_AT) ?: 0L
                )
                transactions.add(transaction)
            }
        }
        return transactions
    }

    // ==================== DEBT METHODS ====================

    fun addDebt(debt: Debt): Long {
        val db = writableDatabase
        val userId = getCurrentUserId()
        if (userId == -1L) {
            Log.e("DatabaseHelper", "No user logged in when adding debt")
            return -1L
        }

        Log.d("DatabaseHelper", "Adding debt for user ID: $userId - ${debt.personName}, ${debt.amount}")

        return try {
            val values = ContentValues().apply {
                put(COLUMN_USER_ID, userId)
                put(COLUMN_PERSON_NAME, debt.personName)
                put(COLUMN_AMOUNT, debt.amount)
                put(COLUMN_DEBT_TYPE, debt.debtType)
                put(COLUMN_DUE_DATE, debt.dueDate)
                put(COLUMN_PAY_DATE, debt.payDate)
                put(COLUMN_DESCRIPTION, debt.description ?: "")
                put(COLUMN_IS_PAID, if (debt.isPaid) 1 else 0)
                put(COLUMN_CREATED_AT, System.currentTimeMillis())
            }

            val result = db.insert(TABLE_DEBTS, null, values)
            Log.d("DatabaseHelper", "Debt insert result: $result")

            if (result == -1L) {
                Log.e("DatabaseHelper", "Failed to insert debt into database")
            } else {
                Log.d("DatabaseHelper", "Debt successfully added with ID: $result")
            }

            result
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error adding debt: ${e.message}", e)
            -1L
        }
    }

    fun getAllDebts(): List<Debt> {
        val debts = mutableListOf<Debt>()
        val userId = getCurrentUserId()
        if (userId == -1L) {
            Log.e("DatabaseHelper", "No user logged in when fetching debts")
            return debts
        }

        val db = readableDatabase
        val cursor = db.query(
            TABLE_DEBTS,
            null,
            "$COLUMN_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null,
            "$COLUMN_CREATED_AT DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val debt = Debt(
                    id = it.getLongOrNull(COLUMN_ID) ?: 0L,
                    personName = it.getStringOrNull(COLUMN_PERSON_NAME) ?: "",
                    amount = it.getDoubleOrNull(COLUMN_AMOUNT) ?: 0.0,
                    debtType = it.getStringOrNull(COLUMN_DEBT_TYPE) ?: "",
                    dueDate = it.getLongOrNull(COLUMN_DUE_DATE) ?: 0L,
                    payDate = it.getLongOrNull(COLUMN_PAY_DATE),
                    description = it.getStringOrNull(COLUMN_DESCRIPTION) ?: "",
                    isPaid = it.getIntOrNull(COLUMN_IS_PAID) == 1,
                    createdAt = it.getLongOrNull(COLUMN_CREATED_AT) ?: 0L
                )
                debts.add(debt)
            }
        }
        Log.d("DatabaseHelper", "Loaded ${debts.size} debts for user $userId")
        return debts
    }

    fun updateDebt(debt: Debt): Int {
        val db = writableDatabase
        val userId = getCurrentUserId()
        if (userId == -1L) return 0

        return try {
            val values = ContentValues().apply {
                put(COLUMN_PERSON_NAME, debt.personName)
                put(COLUMN_AMOUNT, debt.amount)
                put(COLUMN_DEBT_TYPE, debt.debtType)
                put(COLUMN_DUE_DATE, debt.dueDate)
                put(COLUMN_PAY_DATE, debt.payDate)
                put(COLUMN_DESCRIPTION, debt.description ?: "")
                put(COLUMN_IS_PAID, if (debt.isPaid) 1 else 0)
            }
            val result = db.update(TABLE_DEBTS, values, "$COLUMN_ID = ? AND $COLUMN_USER_ID = ?",
                arrayOf(debt.id.toString(), userId.toString()))
            Log.d("DatabaseHelper", "Debt update result: $result rows affected")
            result
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error updating debt: ${e.message}")
            0
        }
    }

    fun deleteDebt(debtId: Long): Int {
        val db = writableDatabase
        val userId = getCurrentUserId()
        if (userId == -1L) return 0

        return try {
            val result = db.delete(TABLE_DEBTS, "$COLUMN_ID = ? AND $COLUMN_USER_ID = ?",
                arrayOf(debtId.toString(), userId.toString()))
            Log.d("DatabaseHelper", "Debt delete result: $result rows affected")
            result
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error deleting debt: ${e.message}")
            0
        }
    }

    // ==================== DEBUG METHODS ====================

    fun debugGetAllDebts(): List<Debt> {
        val debts = mutableListOf<Debt>()
        val userId = getCurrentUserId()
        val db = readableDatabase

        Log.d("DatabaseHelper", "DEBUG: Database query executed for user $userId")

        val cursor = db.query(TABLE_DEBTS, null, "$COLUMN_USER_ID = ?",
            arrayOf(userId.toString()), null, null, "$COLUMN_DUE_DATE ASC")

        Log.d("DatabaseHelper", "DEBUG: Cursor count: ${cursor.count}")

        cursor.use {
            while (it.moveToNext()) {
                val debt = Debt(
                    id = it.getLongOrNull(COLUMN_ID) ?: 0L,
                    personName = it.getStringOrNull(COLUMN_PERSON_NAME) ?: "",
                    amount = it.getDoubleOrNull(COLUMN_AMOUNT) ?: 0.0,
                    debtType = it.getStringOrNull(COLUMN_DEBT_TYPE) ?: "",
                    dueDate = it.getLongOrNull(COLUMN_DUE_DATE) ?: 0L,
                    payDate = it.getLongOrNull(COLUMN_PAY_DATE),
                    description = it.getStringOrNull(COLUMN_DESCRIPTION) ?: "",
                    isPaid = it.getIntOrNull(COLUMN_IS_PAID) == 1,
                    createdAt = it.getLongOrNull(COLUMN_CREATED_AT) ?: 0L
                )
                Log.d("DatabaseHelper", "DEBUG: Found debt in DB - ${debt.personName}, ${debt.amount}, Paid: ${debt.isPaid}")
                debts.add(debt)
            }
        }
        return debts
    }

    fun debugCheckDebtsTable() {
        val db = readableDatabase
        try {
            val cursor = db.rawQuery("PRAGMA table_info($TABLE_DEBTS)", null)
            Log.d("DatabaseHelper", "Debts table structure:")
            cursor.use {
                while (it.moveToNext()) {
                    val name = it.getStringOrNull("name") ?: ""
                    val type = it.getStringOrNull("type") ?: ""
                    Log.d("DatabaseHelper", "Column: $name, Type: $type")
                }
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error checking debts table: ${e.message}")
        }
    }

    // ==================== CLEANUP METHODS ====================

    fun clearUserData(userId: Long): Boolean {
        val db = writableDatabase
        return try {
            db.delete(TABLE_TRANSACTIONS, "$COLUMN_USER_ID = ?", arrayOf(userId.toString()))
            db.delete(TABLE_DEBTS, "$COLUMN_USER_ID = ?", arrayOf(userId.toString()))
            true
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error clearing user data: ${e.message}")
            false
        }
    }

    fun getTransactionCount(): Int {
        val userId = getCurrentUserId()
        if (userId == -1L) return 0

        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_TRANSACTIONS WHERE $COLUMN_USER_ID = ?",
            arrayOf(userId.toString())
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun getDebtCount(): Int {
        val userId = getCurrentUserId()
        if (userId == -1L) return 0

        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_DEBTS WHERE $COLUMN_USER_ID = ?",
            arrayOf(userId.toString())
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun changePassword(userId: Long, currentPassword: String, newPassword: String): Boolean {
        val db = writableDatabase

        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_PASSWORD),
            "$COLUMN_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        val isCurrentPasswordCorrect = cursor.use {
            if (it.moveToFirst()) {
                val storedHash = it.getStringOrNull(COLUMN_PASSWORD)
                if (storedHash != null) {
                    BCrypt.verifyer().verify(currentPassword.toCharArray(), storedHash.toCharArray()).verified
                } else {
                    false
                }
            } else {
                false
            }
        }

        if (!isCurrentPasswordCorrect) {
            return false
        }

        val newHashedPassword = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray())

        val values = ContentValues().apply {
            put(COLUMN_PASSWORD, newHashedPassword)
        }

        val rowsAffected = db.update(
            TABLE_USERS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(userId.toString())
        )

        return rowsAffected > 0
    }

    fun getUserById(userId: Long): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COLUMN_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                User(
                    id = it.getLongOrNull(COLUMN_ID) ?: 0L,
                    email = it.getStringOrNull(COLUMN_EMAIL) ?: "",
                    password = it.getStringOrNull(COLUMN_PASSWORD) ?: "",
                    createdAt = it.getLongOrNull(COLUMN_CREATED_AT) ?: 0L
                )
            } else {
                null
            }
        }
    }

    fun getUserEmail(userId: Long): String? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_EMAIL),
            "$COLUMN_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                it.getStringOrNull(COLUMN_EMAIL)
            } else {
                null
            }
        }
    }
}