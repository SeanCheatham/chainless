import 'package:flutter/material.dart';

class ResponsiveWidget extends StatelessWidget {
  final Widget smallChild;
  final Widget mediumChild;
  final Widget largeChild;

  const ResponsiveWidget(
      {super.key,
      required this.smallChild,
      required this.mediumChild,
      required this.largeChild});

  @override
  Widget build(BuildContext context) {
    final width = MediaQuery.of(context).size.width;
    if (width < 500) {
      return smallChild;
    } else if (width < 800) {
      return mediumChild;
    } else {
      return largeChild;
    }
  }
}
