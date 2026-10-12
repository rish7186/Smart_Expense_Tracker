import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import 'db_helper.dart';
import 'login_screen.dart';
import 'analytics_screen.dart';
import 'dart:io';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  if (Platform.isWindows || Platform.isLinux) {
    sqfliteFfiInit();
    databaseFactory = databaseFactoryFfi;
  }

  runApp(const ExpenseApp());
}

class ExpenseApp extends StatelessWidget {
  const ExpenseApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        fontFamily: 'Roboto',
        scaffoldBackgroundColor: const Color(0xFFF5F7FB),
      ),
      home: const LoginScreen(),
    );
  }
}

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  List<Map<String, dynamic>> expenses = [];
  String selectedFilter = "All";

  @override
  void initState() {
    super.initState();
    loadExpenses();
  }

  void loadExpenses() async {
    try {
      final data = await DBHelper.getExpenses();
      setState(() {
        expenses = data;
      });
    } catch (e) {
      debugPrint("Error loading expenses: $e");
    }
  }

  List<Map<String, dynamic>> getFilteredExpenses() {
    if (selectedFilter == "All") return expenses;

    DateTime now = DateTime.now();

    return expenses.where((e) {
      DateTime date = DateTime.parse(e['date']);

      if (selectedFilter == "Today") {
        return date.day == now.day &&
            date.month == now.month &&
            date.year == now.year;
      }

      if (selectedFilter == "Week") {
        return now.difference(date).inDays <= 7;
      }

      if (selectedFilter == "Month") {
        return date.month == now.month && date.year == now.year;
      }

      return true;
    }).toList();
  }

  double get totalAmount {
    return getFilteredExpenses()
        .fold(0, (sum, item) => sum + item['amount']);
  }

  Color getCategoryColor(String category) {
    switch (category) {
      case 'Food':
        return Colors.orange;
      case 'Travel':
        return Colors.blue;
      case 'Shopping':
        return Colors.purple;
      case 'Bills':
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  IconData getCategoryIcon(String category) {
    switch (category) {
      case 'Food':
        return Icons.restaurant;
      case 'Travel':
        return Icons.flight;
      case 'Shopping':
        return Icons.shopping_bag;
      case 'Bills':
        return Icons.receipt;
      default:
        return Icons.money;
    }
  }

  String getInsight() {
    var list = getFilteredExpenses();
    if (list.isEmpty) return "No data yet";

    Map<String, double> totals = {};
    for (var e in list) {
      totals[e['category']] =
          (totals[e['category']] ?? 0) + e['amount'];
    }

    var highest =
        totals.entries.reduce((a, b) => a.value > b.value ? a : b);

    return "You spend most on ${highest.key}";
  }

  Widget buildChart() {
    Map<String, double> data = {};
    var list = getFilteredExpenses();

    for (var e in list) {
      data[e['category']] =
          (data[e['category']] ?? 0) + e['amount'];
    }

    return Stack(
      alignment: Alignment.center,
      children: [
        SizedBox(
          height: 220,
          child: PieChart(
            PieChartData(
              centerSpaceRadius: 60,
              sections: data.entries.map((e) {
                return PieChartSectionData(
                  value: e.value,
                  color: getCategoryColor(e.key),
                  radius: 50,
                  title: totalAmount > 0
                      ? "${((e.value / totalAmount) * 100).toStringAsFixed(1)}%"
                      : "0%",
                  titleStyle: const TextStyle(
                      fontSize: 10, color: Colors.white),
                );
              }).toList(),
            ),
          ),
        ),
        Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text("Total", style: TextStyle(color: Colors.grey)),
            Text("₹${totalAmount.toStringAsFixed(0)}",
                style: const TextStyle(
                    fontWeight: FontWeight.bold, fontSize: 16)),
          ],
        )
      ],
    );
  }

  void showAddExpenseDialog() {
    String title = '';
    String amount = '';
    String category = 'Food';

    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        shape:
            RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text("Add Expense"),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              decoration: const InputDecoration(labelText: "Title"),
              onChanged: (v) => title = v,
            ),
            TextField(
              decoration: const InputDecoration(labelText: "Amount"),
              keyboardType: TextInputType.number,
              onChanged: (v) => amount = v,
            ),
            DropdownButtonFormField(
              value: category,
              items: ['Food', 'Travel', 'Shopping', 'Bills']
                  .map((e) => DropdownMenuItem(
                        value: e,
                        child: Text(e),
                      ))
                  .toList(),
              onChanged: (v) => category = v!,
            ),
          ],
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text("Cancel")),
          ElevatedButton(
            onPressed: () async {
              double? amt = double.tryParse(amount);
              if (title.isNotEmpty && amt != null) {
                await DBHelper.insertExpense({
                  'title': title,
                  'amount': amt,
                  'category': category,
                  'date': DateTime.now().toString(),
                });
                loadExpenses();
                Navigator.pop(context);
              }
            },
            child: const Text("Add"),
          ),
        ],
      ),
    );
  }

  void deleteExpense(int index) async {
    int id = getFilteredExpenses()[index]['id'];
    await DBHelper.deleteExpense(id);
    loadExpenses();
  }

  Widget buildCard(int index) {
    var e = getFilteredExpenses()[index];

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(18),
        color: Colors.white,
        boxShadow: const [
          BoxShadow(
              color: Colors.black12,
              blurRadius: 8,
              offset: Offset(0, 4))
        ],
      ),
      child: Row(
        children: [
          CircleAvatar(
            backgroundColor:
                getCategoryColor(e['category']).withOpacity(0.2),
            child: Icon(
              getCategoryIcon(e['category']),
              color: getCategoryColor(e['category']),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(e['title'],
                    style: const TextStyle(
                        fontWeight: FontWeight.bold)),
                Text(e['category'],
                    style: const TextStyle(color: Colors.grey)),
              ],
            ),
          ),
          Column(
            children: [
              Text("₹${e['amount']}",
                  style: const TextStyle(
                      fontWeight: FontWeight.bold)),
              IconButton(
                icon: const Icon(Icons.delete, color: Colors.red),
                onPressed: () => deleteExpense(index),
              )
            ],
          )
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    var list = getFilteredExpenses();

    return Scaffold(
      appBar: AppBar(
        title: const Text("Expense Tracker"),
        actions: [
          IconButton(
            icon: const Icon(Icons.bar_chart),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (_) => const AnalyticsScreen()),
              );
            },
          )
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: showAddExpenseDialog,
        child: const Icon(Icons.add),
      ),
      body: Column(
        children: [
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(20),
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                colors: [Colors.black, Colors.blue],
              ),
              borderRadius:
                  BorderRadius.vertical(bottom: Radius.circular(30)),
            ),
            child: Column(
              children: [
                const Text("Total Balance",
                    style: TextStyle(color: Colors.white70)),
                Text("₹${totalAmount.toStringAsFixed(2)}",
                    style: const TextStyle(
                        fontSize: 26,
                        color: Colors.white,
                        fontWeight: FontWeight.bold)),
              ],
            ),
          ),

          const SizedBox(height: 10),

          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: ["All", "Today", "Week", "Month"].map((filter) {
              return ChoiceChip(
                label: Text(filter),
                selected: selectedFilter == filter,
                onSelected: (_) {
                  setState(() {
                    selectedFilter = filter;
                  });
                },
              );
            }).toList(),
          ),

          const SizedBox(height: 5),

          Text(getInsight(),
              style: TextStyle(color: Colors.grey[700])),

          const SizedBox(height: 10),

          if (list.isNotEmpty) buildChart(),

          Expanded(
            child: list.isEmpty
                ? const Center(child: Text("No expenses yet"))
                : ListView.builder(
                    itemCount: list.length,
                    itemBuilder: (_, i) => buildCard(i),
                  ),
          ),
        ],
      ),
    );
  }
}