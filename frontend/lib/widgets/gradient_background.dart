import 'package:chainless_frontend/ui_utils.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:loading_indicator/loading_indicator.dart';

class GradientBackground extends StatelessWidget {
  final Widget child;

  const GradientBackground({super.key, required this.child});
  @override
  Widget build(BuildContext context) => Container(
        constraints: const BoxConstraints.expand(),
        decoration: BoxDecoration(gradient: gradient()),
        child: child,
      );

  static GradientRotation get gradientRotation {
    return const GradientRotation(1.5);
  }

  static gradient() => LinearGradient(
        colors: const [
          Colors.white,
          Color.fromARGB(255, 221, 229, 245),
        ],
        transform: gradientRotation,
      );

  static lowContrastGradient() => LinearGradient(
        colors: const [
          Color.fromARGB(255, 255, 255, 255),
          Color.fromARGB(255, 211, 231, 255),
        ],
        transform: gradientRotation,
      );

  static darkGradient() => LinearGradient(
        colors: const [
          Color.fromARGB(255, 179, 131, 0),
          Color.fromARGB(255, 32, 0, 175),
          Color.fromARGB(255, 0, 69, 206),
        ],
        transform: gradientRotation,
      );
}

// ignore: non_constant_identifier_names
AppBar ChainlessAppBar({Widget? title, List<Widget>? actions}) {
  final titleChildren = <Widget>[
    Image.asset(imageAssetPath("icon-light-310x310.png"), height: 36)
  ];
  if (title != null) {
    titleChildren.add(title.pad4);
  }
  return AppBar(
    title: Row(children: titleChildren),
    actions: actions,
    backgroundColor: Colors.transparent,
    foregroundColor: Colors.white,
  );
}

class ChainlessCard extends StatelessWidget {
  final Widget child;

  const ChainlessCard({super.key, required this.child});

  @override
  Widget build(BuildContext context) =>
      LayoutBuilder(builder: (context, constraints) {
        final radius = constraints.maxWidth / 50;
        final strokeWidth = radius / 2;
        return Container(
          decoration: BoxDecoration(
            gradient: GradientBackground.gradient(),
            borderRadius: BorderRadius.only(
                topRight: Radius.circular(radius),
                bottomLeft: Radius.circular(radius)),
            border: Border(
              left: BorderSide(
                  width: strokeWidth,
                  color: Colors.black,
                  strokeAlign: BorderSide.strokeAlignCenter),
              bottom: BorderSide(
                  width: strokeWidth,
                  color: Colors.black,
                  strokeAlign: BorderSide.strokeAlignCenter),
            ),
          ),
          child: DefaultTextStyle.merge(
              style: const TextStyle(color: Colors.black), child: child),
        );
      });
}

class SmallCard extends StatelessWidget {
  final Widget child;

  const SmallCard({super.key, required this.child});

  @override
  Widget build(BuildContext context) => Container(
        decoration: BoxDecoration(
            borderRadius: const BorderRadius.all(Radius.circular(4)),
            gradient: GradientBackground.lowContrastGradient(),
            boxShadow: [
              BoxShadow(
                  color: Theme.of(context).primaryColorDark,
                  blurRadius: 2,
                  spreadRadius: 0)
            ]),
        child: child,
      );
}

class StatCard extends StatelessWidget {
  final IconData? icon;
  final String title;
  final String value;

  const StatCard(
      {super.key, this.icon, required this.title, required this.value});

  @override
  Widget build(BuildContext context) => SmallCard(child: body(context));

  Widget body(BuildContext context) {
    return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          header(context),
          SizedBox(width: 0, child: Container(height: 16)),
          content(context),
        ]).pad16;
  }

  Widget header(BuildContext context) {
    final children = <Widget>[];
    if (icon != null) {
      children.add(FaIcon(icon!));
      children.add(const VerticalDivider());
    }
    children.add(Text(title,
        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)));
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: children,
    );
  }

  Widget content(BuildContext context) {
    return Text(value, style: const TextStyle(fontWeight: FontWeight.w200));
  }
}

class ChainlessScaffold extends StatelessWidget {
  final PreferredSizeWidget? appBar;
  final Widget? floatingActionButton;
  final FloatingActionButtonLocation? floatingActionButtonLocation;
  final Widget body;

  const ChainlessScaffold(
      {super.key,
      this.appBar,
      this.floatingActionButton,
      this.floatingActionButtonLocation,
      required this.body});

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: appBar,
        floatingActionButton: floatingActionButton,
        extendBodyBehindAppBar: true,
        floatingActionButtonLocation: floatingActionButtonLocation,
        body: background(context),
      );

  Widget background(BuildContext context) => Container(
        constraints: const BoxConstraints.expand(),
        decoration: const BoxDecoration(
          color: Color(0xFF001d3d),
        ),
        child: Padding(
          padding: const EdgeInsets.only(top: 56),
          child: contents(context),
        ),
      );

  Widget contents(BuildContext context) => LayoutBuilder(
          builder: (BuildContext context, BoxConstraints constraints) {
        return CustomScrollView(
          slivers: [
            SliverList(
              delegate: SliverChildListDelegate(
                [body.pad16],
              ),
            ),
            SliverFillRemaining(
              hasScrollBody: false,
              child: Align(
                alignment: Alignment.bottomCenter,
                child: footer(context),
              ),
            )
          ],
        );
      });

  Widget footer(BuildContext context) {
    return Container(
      constraints: const BoxConstraints.tightFor(height: 36),
      decoration: const BoxDecoration(color: Color.fromARGB(255, 105, 71, 71)),
      child: const Center(
          child: Text(
              "This product was made by a man who believes the Ballmer Peak is real. Use at your own risk.",
              style: TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.bold,
                  color: Color.fromARGB(255, 247, 185, 185)))),
    );
  }
}

class ChainlessLoadingIndicator extends StatelessWidget {
  const ChainlessLoadingIndicator({super.key});

  @override
  Widget build(BuildContext context) => ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 300, maxHeight: 300),
        child: const Padding(
          padding: EdgeInsets.all(24.0),
          child: LoadingIndicator(
            indicatorType: Indicator.ballScaleMultiple,
            colors: [
              Color.fromARGB(255, 255, 190, 92),
              Color.fromARGB(255, 152, 187, 255),
              Color.fromARGB(255, 91, 236, 255),
              Color.fromARGB(255, 91, 255, 140),
              Color.fromARGB(255, 255, 91, 173),
              Color.fromARGB(255, 236, 91, 255),
            ],
          ),
        ),
      );
}
