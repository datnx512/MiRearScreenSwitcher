import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../widgets/squircle.dart';

/// First-launch onboarding guide.
/// Shows 3 pages: welcome → Shizuku setup → Quick Settings Tile setup.
class OnboardingPage extends StatefulWidget {
  final VoidCallback onComplete;

  const OnboardingPage({super.key, required this.onComplete});

  @override
  State<OnboardingPage> createState() => _OnboardingPageState();
}

class _OnboardingPageState extends State<OnboardingPage> {
  final PageController _pageController = PageController();
  int _currentPage = 0;

  static const _pages = [
    _OnboardingData(
      icon: '📱',
      title: 'Chào mừng đến MRSS',
      description:
          'Ứng dụng chuyển màn hình sau cho Xiaomi 17 Pro/Pro Max.\n\nKhông cần root — chỉ cần Shizuku.',
    ),
    _OnboardingData(
      icon: '🔐',
      title: 'Cài đặt Shizuku',
      description:
          '1. Tải Shizuku từ shizuku.rikka.app\n2. Khởi động Shizuku qua ADB hoặc无线调试\n3. Mở MRSS và cấp quyền\n\nMRSS cần Shizuku để thực hiện lệnh shell.',
    ),
    _OnboardingData(
      icon: '⚡',
      title: 'Thêm Quick Settings Tile',
      description:
          '1. Kéo xuống Control Center\n2. Nhấn nút chỉnh sửa\n3. Thêm tile "Chuyển sang màn hình sau"\n4. Thêm tile "Chụp màn hình sau"\n\nXong! Nhấn tile để chuyển app sang màn hình sau.',
    ),
  ];

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  Future<void> _complete() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('onboarding_completed', true);
    widget.onComplete();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        width: double.infinity,
        height: double.infinity,
        decoration: const BoxDecoration(gradient: kBrandGradient),
        child: SafeArea(
          child: Column(
            children: [
              // Skip button
              Align(
                alignment: Alignment.topRight,
                child: TextButton(
                  onPressed: _complete,
                  child: const Text(
                    'Bỏ qua',
                    style: TextStyle(color: Colors.white70),
                  ),
                ),
              ),
              // Pages
              Expanded(
                child: PageView.builder(
                  controller: _pageController,
                  itemCount: _pages.length,
                  onPageChanged: (page) {
                    setState(() => _currentPage = page);
                  },
                  itemBuilder: (context, index) {
                    final page = _pages[index];
                    return Padding(
                      padding: const EdgeInsets.all(40),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(
                            page.icon,
                            style: const TextStyle(fontSize: 80),
                          ),
                          const SizedBox(height: 24),
                          Text(
                            page.title,
                            style: const TextStyle(
                              fontSize: 24,
                              fontWeight: FontWeight.bold,
                              color: Colors.white,
                            ),
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 16),
                          Text(
                            page.description,
                            style: const TextStyle(
                              fontSize: 16,
                              color: Colors.white70,
                              height: 1.5,
                            ),
                            textAlign: TextAlign.center,
                          ),
                        ],
                      ),
                    );
                  },
                ),
              ),
              // Page indicators
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(
                  _pages.length,
                  (index) => Container(
                    width: 8,
                    height: 8,
                    margin: const EdgeInsets.symmetric(horizontal: 4),
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: index == _currentPage
                          ? Colors.white
                          : Colors.white30,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 24),
              // Next/Done button
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 40),
                child: SizedBox(
                  width: double.infinity,
                  child: ClipPath(
                    clipper: const SquircleClipper(
                        cornerRadius: SquircleRadii.small),
                    child: Container(
                      decoration: const BoxDecoration(
                          gradient: kBrandGradient),
                      child: ElevatedButton(
                        onPressed: () {
                          if (_currentPage < _pages.length - 1) {
                            _pageController.nextPage(
                              duration:
                                  const Duration(milliseconds: 300),
                              curve: Curves.easeInOut,
                            );
                          } else {
                            _complete();
                          }
                        },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.transparent,
                          foregroundColor: Colors.white,
                          shadowColor: Colors.transparent,
                          padding: const EdgeInsets.symmetric(
                              vertical: 16),
                        ),
                        child: Text(
                          _currentPage < _pages.length - 1
                              ? 'Tiếp theo'
                              : 'Bắt đầu',
                          style: const TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.bold),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 40),
            ],
          ),
        ),
      ),
    );
  }
}

class _OnboardingData {
  final String icon;
  final String title;
  final String description;

  const _OnboardingData({
    required this.icon,
    required this.title,
    required this.description,
  });
}
