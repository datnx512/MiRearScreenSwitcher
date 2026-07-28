import 'package:flutter/material.dart';
import 'squircle.dart';

/// Gradient toggle switch with brand gradient colors.
/// Replaces the default green Switch with a custom animated toggle.
class GradientToggle extends StatefulWidget {
  final bool value;
  final ValueChanged<bool> onChanged;

  const GradientToggle({
    super.key,
    required this.value,
    required this.onChanged,
  });

  @override
  State<GradientToggle> createState() => _GradientToggleState();
}

class _GradientToggleState extends State<GradientToggle> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: () => widget.onChanged(!widget.value),
        onHighlightChanged: (h) => setState(() => _pressed = h),
        customBorder: const SquircleShapeBorder(cornerRadius: SquircleRadii.tiny),
        splashColor: Colors.white.withOpacity(0.2),
        highlightColor: Colors.white.withOpacity(0.1),
        child: ClipPath(
          clipper: const SquircleClipper(cornerRadius: SquircleRadii.tiny),
          child: SizedBox(
            width: 52,
            height: 30,
            child: Stack(
              children: [
                Container(color: Colors.white.withOpacity(0.25)),
                AnimatedOpacity(
                  duration: const Duration(milliseconds: 220),
                  curve: Curves.easeOut,
                  opacity: widget.value ? 1.0 : 0.0,
                  child: Container(
                    decoration: const BoxDecoration(gradient: kBrandGradient),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 4,
                    vertical: 4,
                  ),
                  child: AnimatedAlign(
                    duration: const Duration(milliseconds: 220),
                    curve: Curves.easeOut,
                    alignment: widget.value
                        ? Alignment.centerRight
                        : Alignment.centerLeft,
                    child: AnimatedScale(
                      duration: const Duration(milliseconds: 120),
                      scale: _pressed ? 0.95 : 1.0,
                      child: Container(
                        width: 22,
                        height: 22,
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(11),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withOpacity(0.15),
                              blurRadius: 3,
                              offset: const Offset(0, 1),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Gradient checkbox with brand gradient colors and animation.
class GradientCheckbox extends StatefulWidget {
  final bool value;
  final ValueChanged<bool> onChanged;

  const GradientCheckbox({
    super.key,
    required this.value,
    required this.onChanged,
  });

  @override
  State<GradientCheckbox> createState() => _GradientCheckboxState();
}

class _GradientCheckboxState extends State<GradientCheckbox> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: (_) => setState(() => _pressed = true),
      onTapUp: (_) => setState(() => _pressed = false),
      onTapCancel: () => setState(() => _pressed = false),
      onTap: () => widget.onChanged(!widget.value),
      child: AnimatedScale(
        duration: const Duration(milliseconds: 120),
        scale: _pressed ? 0.9 : 1.0,
        child: ClipPath(
          clipper: const SquircleClipper(cornerRadius: SquircleRadii.checkbox),
          child: SizedBox(
            width: 24,
            height: 24,
            child: Stack(
              children: [
                Container(color: Colors.white.withOpacity(0.25)),
                AnimatedOpacity(
                  duration: const Duration(milliseconds: 200),
                  opacity: widget.value ? 1.0 : 0.0,
                  child: Container(
                    decoration: const BoxDecoration(gradient: kBrandGradient),
                  ),
                ),
                AnimatedOpacity(
                  duration: const Duration(milliseconds: 200),
                  opacity: widget.value ? 0.0 : 1.0,
                  child: CustomPaint(
                    painter: SquircleBorderPainter(
                      radius: SquircleRadii.checkbox,
                      color: Colors.white.withOpacity(0.4),
                      strokeWidth: 2,
                    ),
                  ),
                ),
                Center(
                  child: AnimatedScale(
                    duration: const Duration(milliseconds: 200),
                    curve: Curves.easeOutBack,
                    scale: widget.value ? 1.0 : 0.0,
                    child: const Icon(
                      Icons.check,
                      size: 18,
                      color: Colors.white,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// App list item widget for the app selection page.
class AppListItem extends StatelessWidget {
  final String appName;
  final String packageName;
  final Uint8List? iconBytes;
  final bool isSelected;
  final VoidCallback onToggle;

  const AppListItem({
    super.key,
    required this.appName,
    required this.packageName,
    required this.iconBytes,
    required this.isSelected,
    required this.onToggle,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onToggle,
        splashColor: const Color(0x20FFB5C5),
        highlightColor: const Color(0x10E0B5DC),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
          child: Row(
            children: [
              if (iconBytes != null)
                Image.memory(
                  iconBytes!,
                  width: 48,
                  height: 48,
                  fit: BoxFit.contain,
                  gaplessPlayback: true,
                  filterQuality: FilterQuality.high,
                  isAntiAlias: true,
                )
              else
                const Icon(Icons.android, size: 48, color: Colors.white),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      appName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(color: Colors.white, fontSize: 15),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      packageName,
                      style: const TextStyle(
                        fontSize: 11,
                        color: Colors.white70,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              GradientCheckbox(
                value: isSelected,
                onChanged: (_) => onToggle(),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
