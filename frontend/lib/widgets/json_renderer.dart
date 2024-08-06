import 'package:chainless_frontend/ui_utils.dart';
import 'package:chainless_frontend/widgets/gradient_background.dart';
import 'package:flutter/material.dart';
import 'package:fpdart/fpdart.dart';

class JsonRenderer extends StatelessWidget {
  final dynamic data;

  const JsonRenderer({super.key, this.data});

  @override
  Widget build(BuildContext context) {
    if (data is bool) {
      if (data) {
        return const Icon(Icons.check, color: Colors.green);
      } else {
        return const Icon(Icons.cancel, color: Colors.red);
      }
    } else if (data is num) {
      return Text(data.toString(),
          style: const TextStyle(fontWeight: FontWeight.bold));
    } else if (data is String) {
      return Text(data, style: const TextStyle(fontWeight: FontWeight.w200));
    } else if (data is Map) {
      final m = data as Map<String, dynamic>;
      return Column(
              mainAxisAlignment: MainAxisAlignment.start,
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: m.entries
                  .sortWith(
                      (e) => e.key,
                      Order.from(
                          (a1, a2) => a1.hashCode.compareTo(a2.hashCode)))
                  .toList()
                  .map((e) => ChainlessCard(
                        child: Column(
                            mainAxisAlignment: MainAxisAlignment.start,
                            crossAxisAlignment: CrossAxisAlignment.start,
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(e.key,
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 16,
                                  )),
                              const Divider(),
                              JsonRenderer(data: e.value),
                            ]).pad4,
                      ).pad4)
                  .toList())
          .pad4;
    } else if (data is List) {
      final l = data as List<dynamic>;
      return Column(
        children: l
            .map((e) => ChainlessCard(
                  child: JsonRenderer(data: e),
                ))
            .toList(),
      );
    } else {
      return const Text("-");
    }
  }
}
