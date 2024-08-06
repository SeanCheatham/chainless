import 'package:chainless_frontend/widgets/responsive_widget.dart';
import 'package:code_editor/code_editor.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_highlight/themes/a11y-light.dart';

extension WidgetOps on Widget {
  Widget padScaled(double dimension) => ResponsiveWidget(
      smallChild: Padding(padding: EdgeInsets.all(dimension / 2), child: this),
      mediumChild: Padding(padding: EdgeInsets.all(dimension), child: this),
      largeChild: Padding(padding: EdgeInsets.all(dimension * 2), child: this));
  Widget padScaledHorizontal(double dimension) => ResponsiveWidget(
      smallChild: Padding(
          padding: EdgeInsets.symmetric(horizontal: dimension / 2),
          child: this),
      mediumChild: Padding(
          padding: EdgeInsets.symmetric(horizontal: dimension), child: this),
      largeChild: Padding(
          padding: EdgeInsets.symmetric(horizontal: dimension * 2),
          child: this));
  Widget padScaledVertical(double dimension) => ResponsiveWidget(
      smallChild: Padding(
          padding: EdgeInsets.symmetric(vertical: dimension / 2), child: this),
      mediumChild: Padding(
          padding: EdgeInsets.symmetric(vertical: dimension), child: this),
      largeChild: Padding(
          padding: EdgeInsets.symmetric(vertical: dimension * 2), child: this));

  Widget get pad4 => padScaled(4);
  Widget get pad8 => padScaled(8);
  Widget get pad16 => padScaled(16);
  Widget get pad32 => padScaled(32);

  Widget get pad4Horizontal => padScaledHorizontal(4);
  Widget get pad8Horizontal => padScaledHorizontal(8);
  Widget get pad16Horizontal => padScaledHorizontal(16);

  Widget get pad8Vertical => padScaledVertical(8);
}

const mainColor = Color(0xFF0270E9);

final themeData = ThemeData(
  colorScheme: ColorScheme.fromSeed(
    seedColor: mainColor,
  ),
  useMaterial3: true,
);

final editorTheme = Map.of(a11yLightTheme)
  ..addAll({
    "root": const TextStyle(
        backgroundColor: Colors.transparent, color: Color(0xff545454))
  });

EditorModelStyleOptions codeEditorStyle(double height) =>
    EditorModelStyleOptions(
        heightOfContainer: height,
        editorColor: Colors.transparent,
        editorBorderColor: themeData.dividerColor,
        editorFilenameColor: mainColor,
        editorToolButtonColor: mainColor,
        editorToolButtonTextColor: Colors.white,
        editButtonBackgroundColor: mainColor,
        editButtonTextColor: Colors.black,
        theme: editorTheme,
        textStyleOfTextField: const TextStyle(
          color: Colors.black87,
          backgroundColor: Colors.transparent,
          fontSize: 16,
          letterSpacing: 1.25,
          fontWeight: FontWeight.w500,
        ));

extension StringOverflowOps on String {
  String get ellipsisMiddle {
    if (length <= 12) {
      return this;
    } else {
      final prefix = substring(0, 6);
      final suffix = substring(length - 6);
      return "$prefix...$suffix";
    }
  }
}

final authenticatedNavigatorKey = GlobalKey<NavigatorState>();

String imageAssetPath(String assetName) =>
    kIsWeb ? "assets/$assetName" : assetName;
