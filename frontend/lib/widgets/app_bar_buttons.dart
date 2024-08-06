import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

class DiscordInviteButton extends StatelessWidget {
  static final inviteUrl = Uri.parse("https://discord.gg/pmwzE443gN");

  const DiscordInviteButton({super.key});

  @override
  Widget build(BuildContext context) => IconButton(
        onPressed: () => launchUrl(inviteUrl),
        icon: const Icon(Icons.discord),
        tooltip: "Discord",
      );
}

class DocsButton extends StatelessWidget {
  final String? suffix;
  const DocsButton({super.key, this.suffix});

  @override
  Widget build(BuildContext context) => IconButton(
        onPressed: () => launchUrl(Uri.parse(docsUrlBase + (suffix ?? ""))),
        icon: const Icon(Icons.help),
        tooltip: "Docs",
      );

  static const docsUrlBase = 'https://seancheatham.github.io/chainless';
}
