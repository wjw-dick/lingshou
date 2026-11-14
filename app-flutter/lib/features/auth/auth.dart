import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/config.dart';
import '../../core/http_client.dart';
import '../../core/storage.dart';
import '../../core/theme.dart';
import '../../core/widgets.dart';
import '../../data/api.dart';
import '../../data/models.dart';

class AuthState {
  const AuthState({this.session, this.loading = false, this.error});

  final AuthSession? session;
  final bool loading;
  final String? error;

  bool get isLoggedIn => session != null;
}

final authProvider = StateNotifierProvider<AuthController, AuthState>((ref) {
  return AuthController(
    ref.watch(appApiProvider),
    ref.watch(storageProvider),
  );
});

class AuthController extends StateNotifier<AuthState> {
  AuthController(this._api, this._storage)
      : super(AuthState(session: _storage.session)) {
    _api.setUnauthorizedHandler(_handleUnauthorized);
  }

  final AppApi _api;
  final StorageService _storage;

  Future<bool> login({String? openid, String? nickname}) async {
    final finalOpenid =
        openid == null || openid.trim().isEmpty ? AppConfig.demoOpenid : openid.trim();
    final finalNickname = nickname == null || nickname.trim().isEmpty
        ? AppConfig.demoNickname
        : nickname.trim();
    state = AuthState(session: state.session, loading: true);
    try {
      final session = await _api.login(
        openid: finalOpenid,
        nickname: finalNickname,
      );
      await _storage.saveSession(session);
      state = AuthState(session: session);
      return true;
    } on ApiException catch (error) {
      state = AuthState(session: state.session, error: error.message);
      return false;
    } catch (_) {
      state = const AuthState(error: '登录失败，请稍后重试');
      return false;
    }
  }

  Future<void> logout() async {
    try {
      await _api.logout();
    } catch (_) {
      // 退出登录以本地清理为准，服务端会话失效失败不阻塞用户。
    }
    await _storage.clearSession();
    state = const AuthState();
  }

  void _handleUnauthorized() {
    _storage.clearSession();
    state = const AuthState(error: '登录已过期，请重新登录');
  }
}

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  late final TextEditingController _openidController;
  late final TextEditingController _nicknameController;
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    _openidController = TextEditingController(text: AppConfig.demoOpenid);
    _nicknameController = TextEditingController(text: AppConfig.demoNickname);
  }

  @override
  void dispose() {
    _openidController.dispose();
    _nicknameController.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    if (_submitting) {
      return;
    }
    setState(() => _submitting = true);
    final success = await ref.read(authProvider.notifier).login(
          openid: _openidController.text,
          nickname: _nicknameController.text,
        );
    if (!mounted) {
      return;
    }
    setState(() => _submitting = false);
    if (success) {
      showMessage(context, '登录成功');
      context.go('/home');
    } else {
      final error = ref.read(authProvider).error ?? '登录失败，请稍后重试';
      showMessage(context, error);
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = ref.watch(authProvider);
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFFE7F6ED), Color(0xFFF4F7F5)],
          ),
        ),
        child: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(24, 48, 24, 32),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Container(
                  width: 76,
                  height: 76,
                  decoration: const BoxDecoration(
                    color: AppTheme.primary,
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.storefront,
                    color: Colors.white,
                    size: 40,
                  ),
                ),
                const SizedBox(height: 20),
                const Text(
                  '悦享便利',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 26,
                    fontWeight: FontWeight.w800,
                    color: AppTheme.textPrimary,
                  ),
                ),
                const SizedBox(height: 8),
                const Text(
                  '新鲜便利，随时到家',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: AppTheme.textSecondary),
                ),
                const SizedBox(height: 32),
                Container(
                  padding: const EdgeInsets.all(20),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(20),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.04),
                        blurRadius: 18,
                        offset: const Offset(0, 8),
                      ),
                    ],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Text(
                        '会员登录',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        '演示环境已预填 openid，直接登录即可',
                        style: TextStyle(
                          fontSize: 12,
                          color: AppTheme.textSecondary,
                        ),
                      ),
                      const SizedBox(height: 16),
                      TextField(
                        controller: _openidController,
                        decoration: const InputDecoration(
                          labelText: 'openid',
                          prefixIcon: Icon(Icons.fingerprint),
                        ),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _nicknameController,
                        decoration: const InputDecoration(
                          labelText: '昵称',
                          prefixIcon: Icon(Icons.person_outline),
                        ),
                      ),
                      const SizedBox(height: 20),
                      FilledButton(
                        onPressed: _submitting ? null : _login,
                        child: _submitting
                            ? const SizedBox(
                                width: 20,
                                height: 20,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Colors.white,
                                ),
                              )
                            : const Text('登录'),
                      ),
                      if (auth.isLoggedIn) ...[
                        const SizedBox(height: 10),
                        OutlinedButton(
                          onPressed: () => context.go('/home'),
                          child: const Text('已登录，进入首页'),
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(height: 24),
                const Text(
                  '当前 API 地址',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 12,
                    color: AppTheme.textSecondary,
                  ),
                ),
                const SizedBox(height: 4),
                SelectableText(
                  AppConfig.apiBaseUrl,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppTheme.textSecondary,
                  ),
                ),
                const SizedBox(height: 24),
                const Text(
                  '登录即表示同意《用户协议》与《隐私政策》',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 12,
                    color: AppTheme.textSecondary,
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
