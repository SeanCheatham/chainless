import 'package:chainless_frontend/create_function_page.dart';
import 'package:flutter/material.dart';

class FieldWithHeader extends StatelessWidget {
  final String name;
  final bool required;
  final String tooltip;
  final Widget child;

  const FieldWithHeader(
      {super.key,
      required this.name,
      required this.required,
      required this.tooltip,
      required this.child});

  @override
  Widget build(BuildContext context) {
    final headerChildren = <Widget>[];
    headerChildren.add(Text(name, style: fieldHeader));
    if (required) {
      headerChildren.addAll([
        const VerticalDivider(),
        Tooltip(
            message: "This field is required",
            child: Icon(Icons.circle, size: 14, color: Colors.red[900]))
      ]);
    } else {
      headerChildren.addAll([
        const VerticalDivider(),
        Tooltip(
            message: "This field is optional",
            child: Icon(Icons.circle, size: 14, color: Colors.grey[500]))
      ]);
    }
    headerChildren.addAll([
      const VerticalDivider(),
      Tooltip(
          message: tooltip, child: const Icon(Icons.question_mark, size: 14))
    ]);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [Row(children: headerChildren), child],
    );
  }
}
