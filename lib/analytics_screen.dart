import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import 'db_helper.dart';

class AnalyticsScreen extends StatefulWidget {
  const AnalyticsScreen({super.key});

  @override
  State<AnalyticsScreen> createState() => _AnalyticsScreenState();
}

class _AnalyticsScreenState extends State<AnalyticsScreen> {
  List<Map<String, dynamic>> expenses = [];

  @override
  void initState() {
    super.initState();
    load();
  }

  void load() async {
    expenses = await DBHelper.getExpenses();
    setState(() {});
  }

  Map<String, double> getCategoryTotals() {
    Map<String, double> data = {};
    for (var e in expenses) {
      data[e['category']] =
          (data[e['category']] ?? 0) + e['amount'];
    }
    return data;
  }

  double get total =>
      expenses.fold(0, (sum, e) => sum + e['amount']);

  Color getColor(String category) {
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

  @override
  Widget build(BuildContext context) {
    var data = getCategoryTotals();

    return Scaffold(
      body: Column(
        children: [
          // HEADER
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
                const SizedBox(height: 20),
                const Text("Analytics",
                    style: TextStyle(
                        color: Colors.white,
                        fontSize: 20,
                        fontWeight: FontWeight.bold)),
                Text("₹${total.toStringAsFixed(0)}",
                    style: const TextStyle(
                        color: Colors.white70, fontSize: 16)),
              ],
            ),
          ),

          const SizedBox(height: 20),

          // CHART CARD
          if (data.isNotEmpty)
            Container(
              margin: const EdgeInsets.all(16),
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(20),
                boxShadow: const [
                  BoxShadow(color: Colors.black12, blurRadius: 10)
                ],
              ),
              child: SizedBox(
                height: 200,
                child: PieChart(
                  PieChartData(
                    sections: data.entries.map((e) {
                      return PieChartSectionData(
                        value: e.value,
                        title: "${e.key}",
                        color: getColor(e.key),
                      );
                    }).toList(),
                  ),
                ),
              ),
            )
          else
            const Padding(
              padding: EdgeInsets.all(40.0),
              child: Text("No data for chart"),
            ),

          // LIST
          Expanded(
            child: ListView(
              children: data.entries.map((e) {
                return ListTile(
                  leading: CircleAvatar(
                    backgroundColor: getColor(e.key),
                  ),
                  title: Text(e.key),
                  trailing: Text("₹${e.value.toStringAsFixed(0)}"),
                );
              }).toList(),
            ),
          )
        ],
      ),
    );
  }
}