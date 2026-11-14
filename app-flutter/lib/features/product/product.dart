import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/format.dart';
import '../../core/theme.dart';
import '../../core/widgets.dart';
import '../../data/api.dart';
import '../../data/models.dart';
import '../cart/cart.dart';

typedef ProductQuery = ({int? categoryId, String keyword});

final categoryListProvider = FutureProvider.autoDispose<List<Category>>((ref) {
  return ref.watch(appApiProvider).categories();
});

final productListProvider = FutureProvider.autoDispose
    .family<List<ProductItem>, ProductQuery>((ref, query) {
  return ref.watch(appApiProvider).products(
        categoryId: query.categoryId,
        keyword: query.keyword,
      );
});

final productDetailProvider =
    FutureProvider.autoDispose.family<ProductItem, int>((ref, skuId) {
  return ref.watch(appApiProvider).product(skuId);
});

class ProductListPage extends ConsumerStatefulWidget {
  const ProductListPage({
    super.key,
    this.initialCategoryId,
    this.initialKeyword = '',
  });

  final int? initialCategoryId;
  final String initialKeyword;

  @override
  ConsumerState<ProductListPage> createState() => _ProductListPageState();
}

class _ProductListPageState extends ConsumerState<ProductListPage> {
  late final TextEditingController _searchController;
  late int? _categoryId;
  late String _keyword;

  @override
  void initState() {
    super.initState();
    _categoryId = widget.initialCategoryId;
    _keyword = widget.initialKeyword;
    _searchController = TextEditingController(text: _keyword);
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _search() {
    setState(() => _keyword = _searchController.text.trim());
  }

  void _selectCategory(int? categoryId) {
    if (_categoryId == categoryId) {
      return;
    }
    setState(() => _categoryId = categoryId);
  }

  void _addToCart(ProductItem product) {
    ref.read(cartProvider.notifier).add(product);
    showMessage(context, '已加入购物车');
  }

  @override
  Widget build(BuildContext context) {
    final categoriesAsync = ref.watch(categoryListProvider);
    final productsAsync = ref.watch(
      productListProvider((categoryId: _categoryId, keyword: _keyword)),
    );
    final cartCount = ref.watch(cartProvider).fold<int>(
          0,
          (sum, item) => sum + item.qty,
        );
    return Scaffold(
      appBar: AppBar(
        title: const Text('商品'),
        actions: [
          IconButton(
            onPressed: () => context.push('/cart'),
            icon: Badge(
              isLabelVisible: cartCount > 0,
              label: Text('$cartCount'),
              child: const Icon(Icons.shopping_cart_outlined),
            ),
          ),
          const SizedBox(width: 4),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: TextField(
              controller: _searchController,
              textInputAction: TextInputAction.search,
              onSubmitted: (_) => _search(),
              decoration: InputDecoration(
                hintText: '搜索商品',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: IconButton(
                  onPressed: _search,
                  icon: const Icon(Icons.arrow_forward),
                ),
              ),
            ),
          ),
          SizedBox(
            height: 44,
            child: categoriesAsync.when(
              loading: () => const Center(
                child: SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
              ),
              error: (error, stackTrace) => const SizedBox.shrink(),
              data: (categories) {
                return ListView(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  children: [
                    _CategoryChip(
                      label: '全部',
                      selected: _categoryId == null,
                      onTap: () => _selectCategory(null),
                    ),
                    ...categories.map(
                      (category) => _CategoryChip(
                        label: category.name,
                        selected: _categoryId == category.id,
                        onTap: () => _selectCategory(category.id),
                      ),
                    ),
                  ],
                );
              },
            ),
          ),
          const SizedBox(height: 4),
          Expanded(
            child: productsAsync.when(
              loading: () => const LoadingView(),
              error: (error, stackTrace) => ErrorView(
                message: error.toString(),
                onRetry: () => ref.invalidate(
                  productListProvider(
                    (categoryId: _categoryId, keyword: _keyword),
                  ),
                ),
              ),
              data: (products) {
                if (products.isEmpty) {
                  return const EmptyView(
                    text: '没有找到商品，换个关键词试试',
                    icon: Icons.search_off,
                  );
                }
                return GridView.builder(
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 2,
                    crossAxisSpacing: 12,
                    mainAxisSpacing: 12,
                    childAspectRatio: 0.6,
                  ),
                  itemCount: products.length,
                  itemBuilder: (context, index) {
                    final product = products[index];
                    return ProductCard(
                      product: product,
                      onTap: () => context.push('/products/${product.sku.id}'),
                      onAdd: () => _addToCart(product),
                    );
                  },
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _CategoryChip extends StatelessWidget {
  const _CategoryChip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: ChoiceChip(
        label: Text(label),
        selected: selected,
        onSelected: (_) => onTap(),
        showCheckmark: false,
        selectedColor: AppTheme.primary.withOpacity(0.12),
        labelStyle: TextStyle(
          fontSize: 13,
          color: selected ? AppTheme.primary : AppTheme.textSecondary,
          fontWeight: selected ? FontWeight.w600 : FontWeight.w400,
        ),
        side: BorderSide(
          color: selected ? AppTheme.primary.withOpacity(0.3) : Colors.transparent,
        ),
      ),
    );
  }
}

class ProductDetailPage extends ConsumerStatefulWidget {
  const ProductDetailPage({super.key, required this.skuId});

  final int skuId;

  @override
  ConsumerState<ProductDetailPage> createState() => _ProductDetailPageState();
}

class _ProductDetailPageState extends ConsumerState<ProductDetailPage> {
  int _qty = 1;
  bool _favoriting = false;

  void _addToCart(ProductItem product, {bool goCart = false}) {
    ref.read(cartProvider.notifier).add(product, qty: _qty);
    if (goCart) {
      context.push('/cart');
    } else {
      showMessage(context, '已加入购物车');
    }
  }

  Future<void> _toggleFavorite() async {
    if (_favoriting) {
      return;
    }
    setState(() => _favoriting = true);
    try {
      await ref.read(appApiProvider).toggleFavorite(widget.skuId);
      if (mounted) {
        showMessage(context, '收藏状态已更新');
      }
    } catch (error) {
      if (mounted) {
        showMessage(context, error.toString());
      }
    } finally {
      if (mounted) {
        setState(() => _favoriting = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final productAsync = ref.watch(productDetailProvider(widget.skuId));
    return Scaffold(
      appBar: AppBar(
        title: const Text('商品详情'),
        actions: [
          IconButton(
            onPressed: _favoriting ? null : _toggleFavorite,
            icon: const Icon(Icons.favorite_border),
          ),
          const SizedBox(width: 4),
        ],
      ),
      body: productAsync.when(
        loading: () => const LoadingView(),
        error: (error, stackTrace) => ErrorView(
          message: error.toString(),
          onRetry: () => ref.invalidate(productDetailProvider(widget.skuId)),
        ),
        data: (product) => _buildDetail(product),
      ),
      bottomNavigationBar: productAsync.maybeWhen(
        data: (product) => _buildBottomBar(product),
        orElse: () => const SizedBox.shrink(),
      ),
    );
  }

  Widget _buildDetail(ProductItem product) {
    final spu = product.spu;
    final tags = spu.tags
        .split(RegExp(r'[,，]'))
        .map((tag) => tag.trim())
        .where((tag) => tag.isNotEmpty)
        .toList();
    return ListView(
      padding: const EdgeInsets.only(bottom: 24),
      children: [
        Container(
          color: Colors.white,
          padding: const EdgeInsets.symmetric(vertical: 20),
          child: Center(
            child: ProductImage(
              image: spu.mainImage,
              name: product.name,
              size: 220,
              radius: 18,
            ),
          ),
        ),
        Container(
          color: Colors.white,
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  PriceText(value: product.price, size: 26),
                  const SizedBox(width: 8),
                  if (product.sku.priceRetail > product.sku.priceMember &&
                      product.sku.priceMember > 0)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 3),
                      child: Text(
                        '¥${money(product.sku.priceRetail)}',
                        style: const TextStyle(
                          fontSize: 13,
                          color: AppTheme.textSecondary,
                          decoration: TextDecoration.lineThrough,
                        ),
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 10),
              Text(
                product.name,
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w700,
                  height: 1.35,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                '${product.specText.isEmpty ? '默认规格' : product.specText}${product.unitText.isEmpty ? '' : ' · ${product.unitText}'}',
                style: const TextStyle(
                  fontSize: 13,
                  color: AppTheme.textSecondary,
                ),
              ),
              if (tags.isNotEmpty) ...[
                const SizedBox(height: 10),
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: tags
                      .map(
                        (tag) => Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 3,
                          ),
                          decoration: BoxDecoration(
                            color: AppTheme.primary.withOpacity(0.08),
                            borderRadius: BorderRadius.circular(5),
                          ),
                          child: Text(
                            tag,
                            style: const TextStyle(
                              fontSize: 11,
                              color: AppTheme.primary,
                            ),
                          ),
                        ),
                      )
                      .toList(),
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: 10),
        Container(
          color: Colors.white,
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                '商品信息',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 8),
              _DetailRow(label: '商品编码', value: product.sku.skuCode),
              _DetailRow(label: '条形码', value: product.sku.barcode),
              _DetailRow(
                label: '储存方式',
                value: _storageText(spu.storageCondition),
              ),
              _DetailRow(
                label: '保质期',
                value: spu.shelfLifeDays > 0 ? '${spu.shelfLifeDays} 天' : '暂无',
              ),
              _DetailRow(label: '产地', value: spu.origin),
              _DetailRow(label: '配料', value: spu.ingredients),
              _DetailRow(label: '过敏原', value: spu.allergens),
            ],
          ),
        ),
        const SizedBox(height: 10),
        Container(
          color: Colors.white,
          padding: const EdgeInsets.all(16),
          child: const Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '购买须知',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
              ),
              SizedBox(height: 8),
              Text(
                '· 实际库存以所选门店为准，下单后由门店拣货。\n'
                '· 生鲜商品请尽快食用，如有质量问题请联系门店。\n'
                '· 烟草类商品不参与线上销售。',
                style: TextStyle(
                  fontSize: 13,
                  color: AppTheme.textSecondary,
                  height: 1.7,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildBottomBar(ProductItem product) {
    return SafeArea(
      child: Container(
        padding: const EdgeInsets.fromLTRB(16, 10, 16, 10),
        decoration: const BoxDecoration(
          color: Colors.white,
          border: Border(top: BorderSide(color: Color(0xFFE8EEEA))),
        ),
        child: Row(
          children: [
            QuantityStepper(
              value: _qty,
              onChanged: (value) => setState(() => _qty = value),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: OutlinedButton(
                onPressed: () => _addToCart(product),
                child: const Text('加入购物车'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: FilledButton(
                onPressed: () => _addToCart(product, goCart: true),
                child: const Text('立即购买'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _storageText(String condition) {
    return switch (condition) {
      'COLD' => '冷藏（0-8℃）',
      'FROZEN' => '冷冻（-18℃以下）',
      _ => '常温保存',
    };
  }
}

class _DetailRow extends StatelessWidget {
  const _DetailRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 72,
            child: Text(
              label,
              style: const TextStyle(
                fontSize: 13,
                color: AppTheme.textSecondary,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value.isEmpty ? '暂无' : value,
              style: const TextStyle(fontSize: 13, height: 1.5),
            ),
          ),
        ],
      ),
    );
  }
}
