import 'package:flutter/material.dart';
import 'package:multi_dropdown/multiselect_dropdown.dart';

class ChainSelectionDropdown extends StatelessWidget {
  final List<String> initialSelection;
  final Function(List<String>) selectionUpdated;

  const ChainSelectionDropdown(
      {super.key,
      required this.initialSelection,
      required this.selectionUpdated});
  @override
  Widget build(BuildContext context) => MultiSelectDropDown<String>(
        onOptionSelected: (selectedOptions) =>
            selectionUpdated(selectedOptions.map((v) => v.value!).toList()),
        options: _selectableChainsItems,
        selectionType: SelectionType.multi,
        selectedOptionIcon: const Icon(Icons.check_circle),
        selectedOptions: [
          for (final c in initialSelection) ValueItem(label: c, value: c)
        ],
      );
}

const _selectableChainsItems = [
  ValueItem(label: "bitcoin", value: "bitcoin"),
  ValueItem(label: "ethereum", value: "ethereum"),
];
