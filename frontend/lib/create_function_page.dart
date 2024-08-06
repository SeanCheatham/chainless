import 'dart:convert';

import 'package:chainless_frontend/list_functions_page.dart';
import 'package:chainless_frontend/ui_utils.dart';
import 'package:chainless_frontend/widgets/app_bar_buttons.dart';
import 'package:chainless_frontend/widgets/chain_selection_dropdown.dart';
import 'package:chainless_frontend/widgets/field_with_header.dart';
import 'package:chainless_frontend/widgets/gradient_background.dart';
import 'package:code_editor/code_editor.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:multi_dropdown/multiselect_dropdown.dart';
import 'package:omni_datetime_picker/omni_datetime_picker.dart';
import 'package:provider/provider.dart';

import 'http/api_client.dart';

class CreateFunctionPage extends StatefulWidget {
  const CreateFunctionPage({super.key});

  @override
  State<StatefulWidget> createState() => CreateFunctionPageState();
}

class CreateFunctionPageState extends State<CreateFunctionPage> {
  String name = "MyFunction";
  String language = "js";
  List<String> chains = ["bitcoin", "ethereum"];
  DateTime? initTime;
  String config = "{}";
  late EditorModel configEditorModel;
  PlatformFile? fileResult;

  @override
  void initState() {
    super.initState();
    configEditorModel = EditorModel(styleOptions: codeEditorStyle(300), files: [
      FileEditor(name: "Function Configuration", language: "json", code: config)
    ]);
  }

  @override
  Widget build(BuildContext context) => ChainlessScaffold(
        appBar: ChainlessAppBar(
          title: const Text("Create Function"),
          actions: [
            const DocsButton(suffix: "/docs/persistent-functions"),
            const DiscordInviteButton(),
          ],
        ),
        body: SingleChildScrollView(
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 600),
              child: ChainlessCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    nameField(),
                    divider,
                    languageField(),
                    divider,
                    chainsField(),
                    divider,
                    initTimeField(context),
                    divider,
                    configField(context),
                    divider,
                    pickFileField(context),
                    const Divider(thickness: 2, height: 40),
                    submitButton(context),
                  ],
                ).pad8,
              ),
            ),
          ),
        ),
      );

  static const divider =
      Divider(thickness: 0, height: 40, color: Colors.transparent);

  Widget nameField() => FieldWithHeader(
      name: "Function Name",
      required: true,
      tooltip:
          "A label for your function, to help you distinguish it from other functions in your account.  Should be between 1-63 characters.",
      child: TextField(
          controller:
              TextEditingController.fromValue(TextEditingValue(text: name)),
          onChanged: (v) => name = v));

  Widget languageField() => FieldWithHeader(
        name: "Language / Runtime",
        required: true,
        tooltip: "The programming language used to develop your function.",
        child: MultiSelectDropDown<String>(
          onOptionSelected: (selectedOptions) {
            if (selectedOptions.isNotEmpty) {
              final value = selectedOptions.first.value;
              if (value != null) {
                setState(() {
                  language = selectedOptions.first.value ?? "";
                });
              }
            }
          },
          options: [
            for (final c in selectableLanguages) ValueItem(label: c, value: c)
          ],
          selectionType: SelectionType.single,
          selectedOptionIcon: const Icon(Icons.check_circle),
          selectedOptions: [ValueItem(label: language, value: language)],
        ),
      );

  Widget chainsField() => FieldWithHeader(
        name: "Blockchains",
        required: true,
        tooltip:
            "The blockchains from which your function will receive events.  More than one can be selected.",
        child: ChainSelectionDropdown(
          initialSelection: chains,
          selectionUpdated: (newChains) => setState(() => chains = newChains),
        ),
      );

  Widget initTimeField(BuildContext context) {
    final icon = TextButton.icon(
            onPressed: () => showOmniDateTimePicker(
                    context: context, initialDate: initTime)
                .then((newInitTime) => setState(() => initTime = newInitTime)),
            icon: const Icon(Icons.edit_calendar),
            label: const Text("Set Start Time"))
        .pad8;
    final children = <Widget>[icon];
    if (initTime != null) {
      children.addAll([
        const Divider(
          color: Colors.transparent,
          height: 4,
        ),
        Text(
          initTime?.toIso8601String() ?? "",
          style: const TextStyle(fontStyle: FontStyle.italic),
        ),
      ]);
    }
    return FieldWithHeader(
      name: "Initialization Time",
      required: false,
      tooltip:
          "Select a time in the past to have your function initialize with historical data.",
      child: Column(mainAxisSize: MainAxisSize.min, children: children),
    );
  }

  Widget configField(BuildContext context) => FieldWithHeader(
        name: "Config",
        required: true,
        tooltip:
            "A JSON object containing any values the function needs to initialize the function.",
        child: CodeEditor(
            model: configEditorModel,
            formatters: const ["json"],
            onSubmit: (_, c) {
              setState(() {
                config = c;
              });
            }),
      );

  Widget pickFileField(BuildContext context) {
    final button = TextButton.icon(
      onPressed: () async {
        final result = await FilePicker.platform.pickFiles(
          withData: false,
          withReadStream: true,
        );
        assert(result != null && result.files.isNotEmpty);
        final file = result!.files.first;
        setState(() {
          fileResult = file;
        });
      },
      icon: Icon(Icons.file_open,
          color: fileResult != null ? Colors.green : null),
      label: const Text("Select File"),
    ).pad8;
    return FieldWithHeader(
      name: "Select File",
      required: true,
      tooltip:
          "Choose a file on your computer. For JVM platforms, upload the .jar file.  For other platforms, upload the .zip file containing your code.",
      child: button,
    );
  }

  Widget submitButton(BuildContext context) => Align(
        alignment: Alignment.center,
        child: ElevatedButton.icon(
          onPressed: () => submit(context),
          icon: const Icon(Icons.send),
          label: const Text("Run"),
        ).pad8,
      );

  submit(BuildContext context) async {
    if (name.isEmpty || name.length > 63) {
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text("Invalid function name")));
    } else if (!selectableLanguages.contains(language)) {
      ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("Invalid language selection")));
    } else if (chains.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
          content: Text("Please select at least one blockchain")));
    } else if (fileResult == null) {
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text("Please select a file")));
    } else {
      Navigator.pushAndRemoveUntil(
        context,
        MaterialPageRoute(
            builder: (context) => FunctionCreationInProgress(
                  functionName: name,
                  language: language,
                  chains: chains,
                  initTime: initTime,
                  config: config,
                  file: fileResult!,
                )),
        (_) => false,
      );
    }
  }
}

class FunctionCreationInProgress extends StatefulWidget {
  final String functionName;
  final String language;
  final List<String> chains;
  final DateTime? initTime;
  final String config;
  final PlatformFile file;

  const FunctionCreationInProgress({
    super.key,
    required this.functionName,
    required this.language,
    required this.chains,
    this.initTime,
    required this.config,
    required this.file,
  });

  @override
  State<StatefulWidget> createState() => FunctionCreationInProgressState();
}

class FunctionCreationInProgressState
    extends State<FunctionCreationInProgress> {
  @override
  Widget build(BuildContext context) {
    return ChainlessScaffold(
      body: Center(
        child: ChainlessCard(
          child: StreamBuilder(
            stream: statusStream(context.watch<PublicApiClient>()),
            builder: (context, snapshot) => snapshot.hasData
                ? snapshot.data!(context)
                : const Text("Creating Function", style: messageStyle),
          ).pad32,
        ),
      ),
    );
  }

  Stream<Widget Function(BuildContext)> statusStream(
      PublicApiClient client) async* {
    goBack(BuildContext context) => TextButton.icon(
        onPressed: () => Navigator.pushAndRemoveUntil(
              context,
              MaterialPageRoute(
                builder: (context) => const ListFunctionsPage(),
                maintainState: false,
              ),
              (_) => false,
            ),
        icon: const Icon(Icons.arrow_back),
        label: const Text("Back"));
    try {
      final functionId = await client.create(
          widget.functionName, widget.language, widget.chains);
      yield (_) => const Text("Uploading Function", style: messageStyle);
      await client.upload(
          functionId, widget.file.readStream!, widget.file.size);
      yield (_) => const Text("Initializing Function", style: messageStyle);
      final jsonConfig = json.decode(widget.config);
      final initTimestamp = widget.initTime?.millisecondsSinceEpoch;
      await client.init(functionId, jsonConfig, initTimestamp);
      yield (context) => Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            mainAxisAlignment: MainAxisAlignment.center,
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text("Done", style: messageStyle),
              goBack(context),
            ],
          );
    } catch (e) {
      yield (context) => Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            mainAxisAlignment: MainAxisAlignment.center,
            mainAxisSize: MainAxisSize.min,
            children: [
              const Row(mainAxisSize: MainAxisSize.min, children: [
                Text("A horrible tragedy has occurred!", style: messageStyle),
                VerticalDivider(),
                FaIcon(FontAwesomeIcons.faceFrown, size: 36),
              ]),
              goBack(context),
            ],
          );
    }
  }

  static const messageStyle =
      TextStyle(fontSize: 36, fontWeight: FontWeight.bold);
}

const selectableLanguages = ["js", "jvm"];

const fieldHeader = TextStyle(
  fontSize: 18,
  fontWeight: FontWeight.bold,
);
