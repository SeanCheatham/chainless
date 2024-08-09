import 'package:chainless_frontend/http/api_client.dart';
import 'package:chainless_frontend/widgets/gradient_background.dart';
import 'package:chainless_frontend/widgets/json_renderer.dart';
import 'package:chainless_frontend/models/models.dart';
import 'package:chainless_frontend/ui_utils.dart';
import 'package:date_time_format/date_time_format.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:fpdart/fpdart.dart' hide State;
import 'package:omni_datetime_picker/omni_datetime_picker.dart';
import 'package:provider/provider.dart';
import 'package:syncfusion_flutter_charts/charts.dart';
import 'dart:math';

class FunctionView extends StatelessWidget {
  final String functionId;
  final FunctionInfo function;
  final bool showRecords;

  const FunctionView(
      {super.key,
      required this.functionId,
      required this.function,
      this.showRecords = true});

  @override
  Widget build(BuildContext context) {
    final children = <Widget>[
      header(function),
      const Divider(thickness: 0, color: Colors.transparent),
      state(function),
    ];
    if (showRecords) {
      children.add(const Divider(thickness: 0, color: Colors.transparent));
      children.add(FunctionRecordsViewWithDateTime(
        functionId: functionId,
      ));
    }
    return Column(children: [
      ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 800),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisSize: MainAxisSize.min,
          children: children,
        ),
      ),
    ]);
  }

  Widget header(FunctionInfo function) {
    final children = <Widget>[];
    children.add(Row(
      mainAxisAlignment: MainAxisAlignment.start,
      children: [
        languageIcon(
          function.language,
          size: 32,
          color: Colors.black,
        ),
        const VerticalDivider(width: 24),
        Text(function.name,
            style: const TextStyle(
              fontWeight: FontWeight.bold,
              color: Colors.black,
              fontSize: 24,
            ))
      ],
    ).pad8);
    children.add(const Divider());
    for (final entry in function.chainStates.entries.toList()) {
      final chainName = entry.key;
      final blockId = entry.value.ellipsisMiddle;
      final tile = Row(children: [
        FaIcon(chainIcon(chainName), color: Colors.black).pad8Horizontal,
        Text(chainName,
                style: const TextStyle(
                    fontWeight: FontWeight.bold, color: Colors.black))
            .pad8Horizontal,
        Expanded(child: Container()),
        Align(
          alignment: Alignment.centerRight,
          child: Text(blockId, style: const TextStyle(color: Colors.black))
              .pad8Horizontal,
        ),
      ]).pad8Vertical;
      children.add(tile);
    }
    return ChainlessCard(
        child: Column(mainAxisSize: MainAxisSize.min, children: children));
  }

  Widget state(FunctionInfo function) {
    final children = <Widget>[
      const Text("State",
              style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold))
          .pad8,
      ConstrainedBox(
        constraints: const BoxConstraints(maxHeight: 400),
        child: SingleChildScrollView(
          child: JsonRenderer(data: function.state),
        ),
      )
    ];
    return ChainlessCard(
        child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: children));
  }
}

class FunctionRecordsViewWithDateTime extends StatefulWidget {
  final String functionId;

  const FunctionRecordsViewWithDateTime({super.key, required this.functionId});

  @override
  State<StatefulWidget> createState() => FunctionRecordsViewWithDateTimeState();
}

class FunctionRecordsViewWithDateTimeState
    extends State<FunctionRecordsViewWithDateTime> {
  DateTime after = DateTime.now().subtract(const Duration(hours: 1));
  DateTime before = DateTime.now();

  @override
  Widget build(BuildContext context) => FutureBuilder(
      future: context
          .watch<PublicApiClient>()
          .functionInvocationRecords(
            widget.functionId,
            after: after,
            before: before,
          )
          .toList(),
      builder: (context, snapshot) => snapshot.hasData
          ? FunctionRecordsView(
              records: snapshot.data!,
              after: after,
              before: before,
              onAfterChanged: (a) => setState(() => after = a),
              onBeforeChanged: (b) => setState(() => before = b),
            )
          : loading);

  Widget get loading => const ChainlessLoadingIndicator();
}

class FunctionRecordsView extends StatelessWidget {
  final List<FunctionInvocationRecord> records;
  final DateTime after;
  final DateTime before;
  final Function(DateTime) onAfterChanged;
  final Function(DateTime) onBeforeChanged;

  const FunctionRecordsView(
      {super.key,
      required this.records,
      required this.after,
      required this.before,
      required this.onAfterChanged,
      required this.onBeforeChanged});

  @override
  Widget build(BuildContext context) => ChainlessCard(
        child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              headerText(),
              timePickers(context).pad8Vertical,
              const Divider(),
              invocationCount.pad8Vertical,
              computeDuration.pad8Vertical,
              chart.pad8Vertical,
            ]),
      );

  static const numberHeaderStyle = TextStyle(fontWeight: FontWeight.bold);
  static const numberValueStyle =
      TextStyle(fontSize: 24, fontWeight: FontWeight.w200);

  Widget headerText() => const Text("Statistics",
          style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold))
      .pad8;

  Widget timePickers(BuildContext context) {
    final children = <Widget>[];
    children.add(Row(children: [
      const Text("Date Range Start",
          style: TextStyle(fontWeight: FontWeight.bold)),
      Expanded(child: Container()),
      TextButton.icon(
        onPressed: () =>
            showOmniDateTimePicker(context: context, initialDate: after)
                .then((v) {
          if (v != null) onAfterChanged(v);
        }),
        icon: const Icon(Icons.edit),
        label: Text(
            DateTimeFormat.format(after, format: DateTimeFormats.american)),
      ),
    ]));
    children.add(Row(children: [
      const Text("Date Range End",
          style: TextStyle(fontWeight: FontWeight.bold)),
      Expanded(child: Container()),
      TextButton.icon(
        onPressed: () =>
            showOmniDateTimePicker(context: context, initialDate: before)
                .then((v) {
          if (v != null) onBeforeChanged(v);
        }),
        icon: const Icon(Icons.edit),
        label: Text(
            DateTimeFormat.format(before, format: DateTimeFormats.american)),
      ),
    ]));
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: children,
    ).pad16Horizontal;
  }

  Widget get invocationCount {
    final children = <Widget>[
      const FaIcon(FontAwesomeIcons.computer),
      const VerticalDivider(),
      const Text("Invocation Count",
          style: TextStyle(fontWeight: FontWeight.bold)),
      Expanded(child: Container()),
      Text(records.length.toString()),
    ];
    return Row(children: children).pad8.pad8Horizontal;
  }

  Widget get computeDuration {
    final total = records.fold(const Duration(milliseconds: 0),
        (previousValue, element) => previousValue + element.activeDuration);
    final text = "${total.inSeconds} seconds";
    final children = <Widget>[
      const FaIcon(FontAwesomeIcons.clock),
      const VerticalDivider(),
      const Text("Total Compute Duration",
          style: TextStyle(fontWeight: FontWeight.bold)),
      Expanded(child: Container()),
      Text(text),
    ];
    return Row(children: children).pad8.pad8Horizontal;
  }

  Widget get chart => LayoutBuilder(
        builder: (context, constraints) => SfCartesianChart(
          primaryXAxis: const CategoryAxis(
            title: AxisTitle(text: "Time"),
          ),
          primaryYAxis: const NumericAxis(
            title: AxisTitle(text: "Duration (ms)"),
          ),
          series: series(constraints.maxWidth ~/ 60),
        ).pad8,
      );

  List<ColumnSeries<(DateTime, Duration), String>> series(int bucketCount) =>
      <ColumnSeries<(DateTime, Duration), String>>[
        ColumnSeries(
          dataSource: timeBuckets(bucketCount),
          xValueMapper: (info, _) =>
              DateTimeFormat.format(info.$1, format: DateTimeFormats.american),
          yValueMapper: (info, _) => info.$2.inMilliseconds,
          name: "Compute Duration",
          gradient: const LinearGradient(
            colors: [
              Color.fromARGB(255, 32, 0, 175),
              Color.fromARGB(255, 0, 69, 206),
            ],
            transform: GradientRotation(pi / 2),
          ),
        )
      ];

  List<(DateTime, Duration)> timeBuckets(int bucketCount) {
    final entries = records
        .map((r) => (r.endTime, r.activeDuration))
        .toList()
        .sortWithDate((instance) => instance.$1);
    if (entries.length < bucketCount) return entries;
    final low = entries.first.$1;
    final high = entries.last.$1;
    final deltaMs = high.difference(low).inMilliseconds + 1;
    final buckets = List.filled(bucketCount, Duration.zero);
    for (final (time, duration) in entries) {
      final entryDelta = time.difference(low);
      final bucket =
          (entryDelta.inMilliseconds / deltaMs * bucketCount).floor();
      buckets[bucket] += duration;
    }
    return List.generate(
        bucketCount,
        (i) => (
              DateTime.fromMillisecondsSinceEpoch(
                  low.millisecondsSinceEpoch + i * deltaMs ~/ bucketCount),
              buckets[i]
            ));
  }
}

Widget languageIcon(String language, {Color? color, double? size}) {
  switch (language) {
    case "js":
      return Tooltip(
          message: "NodeJS",
          child: FaIcon(FontAwesomeIcons.nodeJs, color: color, size: size));
    case "jvm":
      return Tooltip(
          message: "JVM",
          child: FaIcon(FontAwesomeIcons.java, color: color, size: size));
    default:
      return Tooltip(
          message: "Unknown",
          child: Icon(Icons.question_mark, color: color, size: size));
  }
}

IconData chainIcon(String chain, {Color? color, double? size}) {
  switch (chain) {
    case "bitcoin":
      return FontAwesomeIcons.bitcoin;
    case "ethereum":
      return FontAwesomeIcons.ethereum;
    case "apparatus":
      return FontAwesomeIcons.arrowRightToBracket;
    default:
      return FontAwesomeIcons.question;
  }
}
