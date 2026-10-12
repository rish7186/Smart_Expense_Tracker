import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';

class DBHelper {
  static Database? _database;

  static Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await initDB();
    return _database!;
  }

  static Future<Database> initDB() async {
    final path = join(await getDatabasesPath(), 'expenses.db');

    return openDatabase(
      path,
      version: 1,
      onCreate: (db, version) {
        return db.execute('''
          CREATE TABLE expenses(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT,
            amount REAL,
            category TEXT,
            date TEXT
          )
        ''');
      },
    );
  }

  static Future<void> insertExpense(Map<String, dynamic> data) async {
    final db = await database;
    await db.insert('expenses', data);
  }

  static Future<List<Map<String, dynamic>>> getExpenses() async {
    final db = await database;
    return db.query('expenses');
  }

  static Future<void> deleteExpense(int id) async {
    final db = await database;
    await db.delete('expenses', where: 'id = ?', whereArgs: [id]);
  }
}