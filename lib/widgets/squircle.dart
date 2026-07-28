import 'package:flutter/material.dart';
import 'dart:math' as math;

// Brand gradient colors used across the app
const List<Color> kGradientColors = [
  Color(0xFFFF9D88), // Coral orange
  Color(0xFFFFB5C5), // Pink
  Color(0xFFE0B5DC), // Purple
  Color(0xFFA8C5E5), // Blue
];

const LinearGradient kBrandGradient = LinearGradient(
  begin: Alignment.topLeft,
  end: Alignment.bottomRight,
  colors: kGradientColors,
);

/// Squircle (superellipse) radii constants.
/// Based on physical screen corner radius 16.4mm, superellipse exponent n=2.84.
class SquircleRadii {
  static const double large = 32.0;
  static const double medium = 24.0;
  static const double small = 12.0;
  static const double tiny = 16.0;
  static const double checkbox = 6.0;
}

/// Precise superellipse (squircle) shape border for InkWell ripples.
/// Uses n=2.84 exponent for smooth curves matching screen corners.
class SquircleShapeBorder extends ShapeBorder {
  final double cornerRadius;
  static const double n = 2.84;

  const SquircleShapeBorder({required this.cornerRadius});

  @override
  EdgeInsetsGeometry get dimensions => EdgeInsets.zero;

  @override
  Path getInnerPath(Rect rect, {TextDirection? textDirection}) {
    return _createSquirclePath(rect.size, cornerRadius);
  }

  @override
  Path getOuterPath(Rect rect, {TextDirection? textDirection}) {
    return _createSquirclePath(rect.size, cornerRadius);
  }

  @override
  void paint(Canvas canvas, Rect rect, {TextDirection? textDirection}) {}

  @override
  ShapeBorder scale(double t) =>
      SquircleShapeBorder(cornerRadius: cornerRadius * t);

  static Path _createSquirclePath(Size size, double radius) {
    final double width = size.width;
    final double height = size.height;
    final double effectiveRadius = radius.clamp(
      0.0,
      math.min(width, height) / 2,
    );

    final path = Path();

    path.moveTo(0, effectiveRadius);
    for (double t = 0; t <= 1.0; t += 0.02) {
      final angle = (1 - t) * math.pi / 2;
      final x = effectiveRadius *
          (1 -
              math.pow(math.cos(angle).abs(), 2 / n) *
                  (math.cos(angle) >= 0 ? 1 : -1));
      final y = effectiveRadius *
          (1 -
              math.pow(math.sin(angle).abs(), 2 / n) *
                  (math.sin(angle) >= 0 ? 1 : -1));
      path.lineTo(x, y);
    }

    path.lineTo(width - effectiveRadius, 0);

    for (double t = 0; t <= 1.0; t += 0.02) {
      final angle = t * math.pi / 2;
      final x = width -
          effectiveRadius *
              (1 -
                  math.pow(math.cos(angle).abs(), 2 / n) *
                      (math.cos(angle) >= 0 ? 1 : -1));
      final y = effectiveRadius *
          (1 -
              math.pow(math.sin(angle).abs(), 2 / n) *
                  (math.sin(angle) >= 0 ? 1 : -1));
      path.lineTo(x, y);
    }

    path.lineTo(width, height - effectiveRadius);

    for (double t = 0; t <= 1.0; t += 0.02) {
      final angle = (1 - t) * math.pi / 2 + math.pi / 2;
      final x = width -
          effectiveRadius *
              (1 -
                  math.pow(math.cos(angle).abs(), 2 / n) *
                      (math.cos(angle) >= 0 ? 1 : -1));
      final y = height -
          effectiveRadius *
              (1 -
                  math.pow(math.sin(angle).abs(), 2 / n) *
                      (math.sin(angle) >= 0 ? 1 : -1));
      path.lineTo(x, y);
    }

    path.lineTo(effectiveRadius, height);

    for (double t = 0; t <= 1.0; t += 0.02) {
      final angle = t * math.pi / 2 + math.pi;
      final x = effectiveRadius *
          (1 -
              math.pow(math.cos(angle).abs(), 2 / n) *
                  (math.cos(angle) >= 0 ? 1 : -1));
      final y = height -
          effectiveRadius *
              (1 -
                  math.pow(math.sin(angle).abs(), 2 / n) *
                      (math.sin(angle) >= 0 ? 1 : -1));
      path.lineTo(x, y);
    }

    path.close();
    return path;
  }
}

/// Precise superellipse clipper.
class SquircleClipper extends CustomClipper<Path> {
  final double cornerRadius;
  static const double n = 2.84;

  const SquircleClipper({required this.cornerRadius});

  @override
  Path getClip(Size size) {
    return _createSquirclePath(size, cornerRadius);
  }

  Path _createSquirclePath(Size size, double radius) {
    final w = size.width;
    final h = size.height;
    final r = radius;

    final path = Path();

    path.moveTo(0, r);
    _drawSquircleArc(path, r, r, r, math.pi, math.pi * 1.5);
    path.lineTo(w - r, 0);
    _drawSquircleArc(path, w - r, r, r, math.pi * 1.5, math.pi * 2);
    path.lineTo(w, h - r);
    _drawSquircleArc(path, w - r, h - r, r, 0, math.pi * 0.5);
    path.lineTo(r, h);
    _drawSquircleArc(path, r, h - r, r, math.pi * 0.5, math.pi);
    path.close();
    return path;
  }

  void _drawSquircleArc(
    Path path,
    double cx,
    double cy,
    double radius,
    double startAngle,
    double endAngle,
  ) {
    const int segments = 30;

    for (int i = 0; i <= segments; i++) {
      final t = i / segments;
      final angle = startAngle + (endAngle - startAngle) * t;

      final cosA = math.cos(angle);
      final sinA = math.sin(angle);

      final x = cx + radius * _sgn(cosA) * math.pow(cosA.abs(), 2.0 / n);
      final y = cy + radius * _sgn(sinA) * math.pow(sinA.abs(), 2.0 / n);

      path.lineTo(x, y);
    }
  }

  double _sgn(double x) => x < 0 ? -1.0 : 1.0;

  @override
  bool shouldReclip(SquircleClipper oldClipper) =>
      oldClipper.cornerRadius != cornerRadius;
}

/// Precise superellipse border painter.
class SquircleBorderPainter extends CustomPainter {
  final double radius;
  final Color color;
  final double strokeWidth;
  static const double n = 2.84;

  const SquircleBorderPainter({
    required this.radius,
    required this.color,
    required this.strokeWidth,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth;

    final path = _createSquirclePath(size, radius);
    canvas.drawPath(path, paint);
  }

  Path _createSquirclePath(Size size, double r) {
    final w = size.width;
    final h = size.height;

    final path = Path();
    path.moveTo(0, r);

    _drawSquircleArc(path, r, r, r, math.pi, math.pi * 1.5);
    path.lineTo(w - r, 0);

    _drawSquircleArc(path, w - r, r, r, math.pi * 1.5, math.pi * 2);
    path.lineTo(w, h - r);

    _drawSquircleArc(path, w - r, h - r, r, 0, math.pi * 0.5);
    path.lineTo(r, h);

    _drawSquircleArc(path, r, h - r, r, math.pi * 0.5, math.pi);

    path.close();
    return path;
  }

  void _drawSquircleArc(
    Path path,
    double cx,
    double cy,
    double radius,
    double startAngle,
    double endAngle,
  ) {
    const int segments = 30;
    for (int i = 0; i <= segments; i++) {
      final t = i / segments;
      final angle = startAngle + (endAngle - startAngle) * t;
      final cosA = math.cos(angle);
      final sinA = math.sin(angle);
      final x = cx + radius * _sgn(cosA) * math.pow(cosA.abs(), 2.0 / n);
      final y = cy + radius * _sgn(sinA) * math.pow(sinA.abs(), 2.0 / n);
      path.lineTo(x, y);
    }
  }

  double _sgn(double x) => x < 0 ? -1.0 : 1.0;

  @override
  bool shouldRepaint(SquircleBorderPainter oldDelegate) {
    return oldDelegate.radius != radius ||
        oldDelegate.color != color ||
        oldDelegate.strokeWidth != strokeWidth;
  }
}
