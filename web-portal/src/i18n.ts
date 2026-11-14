import { computed, ref } from 'vue'
import type { SiteMenu } from '@/types'

export type Lang = 'zh' | 'en'

const stored = typeof localStorage !== 'undefined' ? localStorage.getItem('yx-lang') : null
const langRef = ref<Lang>(stored === 'en' ? 'en' : 'zh')

const dict: Record<string, { zh: string; en: string }> = {
  'brand.name': { zh: '悦享便利', en: 'Yuexiang Mart' },
  'brand.tagline': { zh: '社区 5 分钟生活圈', en: 'Your five-minute neighborhood' },

  'nav.home': { zh: '首页', en: 'Home' },
  'nav.stores': { zh: '门店查询', en: 'Stores' },
  'nav.products': { zh: '鲜食与商品', en: 'Fresh & Goods' },
  'nav.franchise': { zh: '加盟合作', en: 'Franchise' },
  'nav.news': { zh: '品牌资讯', en: 'Journal' },
  'nav.contact': { zh: '联系我们', en: 'Contact' },

  'common.more': { zh: '了解更多', en: 'Learn more' },
  'common.viewAll': { zh: '查看全部', en: 'View all' },
  'common.back': { zh: '返回', en: 'Back' },
  'common.close': { zh: '关闭', en: 'Close' },
  'common.retry': { zh: '重新加载', en: 'Try again' },
  'common.loading': { zh: '加载中', en: 'Loading' },
  'common.empty': { zh: '暂无内容', en: 'Nothing here yet' },
  'common.error': { zh: '内容加载失败', en: 'Failed to load' },
  'common.call': { zh: '电话', en: 'Call' },
  'common.navigate': { zh: '导航', en: 'Directions' },
  'common.share': { zh: '分享', en: 'Share' },
  'common.miniProgram': { zh: '小程序下单', en: 'Order in mini program' },
  'common.minutes': { zh: '分钟', en: 'min' },
  'common.storeUnit': { zh: '家门店', en: 'stores' },
  'common.skuUnit': { zh: '款鲜食', en: 'fresh items' },
  'common.levelUnit': { zh: '级会员', en: 'member tiers' },
  'common.regionUnit': { zh: '个城区', en: 'districts' },

  'lang.zh': { zh: '中文', en: '中文' },
  'lang.en': { zh: 'EN', en: 'EN' },

  'footer.desc': {
    zh: '悦享便利相信，好的便利店是社区的一部分。我们用鲜食、自有品牌与数字化服务，把日常过得更好一点。',
    en: 'We believe a good convenience store belongs to its neighborhood. Fresh food, private labels and thoughtful digital service — making the everyday a little better.'
  },
  'footer.explore': { zh: '探索', en: 'Explore' },
  'footer.partner': { zh: '合作', en: 'Partner' },
  'footer.contact': { zh: '联系', en: 'Contact' },
  'footer.service': { zh: '客服热线', en: 'Service hotline' },
  'footer.hours': { zh: '营业时间', en: 'Opening hours' },
  'footer.icp': { zh: '备案号', en: 'ICP licence' },
  'footer.rights': { zh: '保留所有权利', en: 'All rights reserved' },
  'footer.slogan': { zh: '把日常，过得好一点。', en: 'A better everyday, around the corner.' },

  'seo.homeTitle': { zh: '悦享便利 — 社区 5 分钟生活圈', en: 'Yuexiang Mart — Your Five-Minute Neighborhood' },
  'seo.homeDesc': {
    zh: '悦享便利是扎根上海及长三角的社区便利连锁品牌。鲜食现制、自有品牌、30 分钟送达，让社区生活更从容。',
    en: 'Yuexiang Mart is a neighborhood convenience brand across Shanghai and the Yangtze River Delta — fresh food, private labels and 30-minute delivery.'
  },
  'seo.storesTitle': { zh: '门店查询 — 悦享便利', en: 'Find a Store — Yuexiang Mart' },
  'seo.storesDesc': {
    zh: '查询悦享便利在上海各城区的门店地址、营业时间与配送范围，一键导航或致电门店。',
    en: 'Find Yuexiang Mart stores near you: addresses, opening hours, delivery coverage, directions and phone numbers.'
  },
  'seo.productsTitle': { zh: '鲜食与商品 — 悦享便利', en: 'Fresh & Goods — Yuexiang Mart' },
  'seo.productsDesc': {
    zh: '浏览悦享便利今日鲜食、饮料、零食与日用品，查看价格、会员价、保质期与贮存方式。',
    en: 'Explore fresh food, drinks, snacks and daily goods at Yuexiang Mart, with prices, member prices, shelf life and storage details.'
  },
  'seo.franchiseTitle': { zh: '加盟合作 — 悦享便利', en: 'Franchise — Yuexiang Mart' },
  'seo.franchiseDesc': {
    zh: '加入悦享便利社区便利网络，获得选址评估、供应链、数字化系统与门店培训的一体化支持。',
    en: 'Join the Yuexiang Mart network with end-to-end support in site selection, supply chain, digital systems and store training.'
  },
  'seo.newsTitle': { zh: '品牌资讯 — 悦享便利', en: 'Journal — Yuexiang Mart' },
  'seo.newsDesc': { zh: '悦享便利品牌动态、鲜食上新与加盟资讯。', en: 'Brand news, fresh food launches and franchise updates from Yuexiang Mart.' },
  'seo.contactTitle': { zh: '联系我们 — 悦享便利', en: 'Contact — Yuexiang Mart' },
  'seo.contactDesc': {
    zh: '联系悦享便利：客服热线、营业时间、门店地址、加盟合作与在线留言。',
    en: 'Contact Yuexiang Mart: service hotline, opening hours, store addresses, franchise enquiries and messages.'
  },

  'home.eyebrow': { zh: '社区便利 · 自 2018', en: 'Neighborhood convenience · since 2018' },
  'home.line1': { zh: '社区', en: 'Your' },
  'home.line2': { zh: '5 分钟生活圈', en: 'five-minute' },
  'home.line3': { zh: '', en: 'neighborhood' },
  'home.desc': {
    zh: '从一份热三明治到深夜的一杯咖啡，我们把新鲜、温暖与安心，放进你每天都会经过的那条街。',
    en: 'From a warm sandwich to a late-night coffee, we put freshness, warmth and ease on the street you walk every day.'
  },
  'home.explore': { zh: '寻找附近门店', en: 'Find a store' },
  'home.join': { zh: '加入我们', en: 'Partner with us' },
  'home.scroll': { zh: '向下探索', en: 'Scroll' },
  'home.statStores': { zh: '在营门店', en: 'Open stores' },
  'home.statRegions': { zh: '覆盖城区', en: 'Districts' },
  'home.statFresh': { zh: '鲜食 SKU', en: 'Fresh items' },
  'home.statLevels': { zh: '会员等级', en: 'Member tiers' },
  'home.brandPromise': { zh: '新鲜 · 就近 · 有温度', en: 'Fresh · Near · Human' },
  'home.freshIndex': { zh: '01 / 鲜食', en: '01 / Fresh' },
  'home.freshTitle': { zh: '鲜食，是便利店的灵魂', en: 'Fresh food is the soul of a convenience store' },
  'home.freshDesc': {
    zh: '每日现制，短保直达。热压三明治、关东煮与现磨咖啡，在每一个匆忙的早晨和加班的夜里，给你一份刚刚好的照顾。',
    en: 'Made daily, delivered short-dated. Hot-pressed sandwiches, oden and freshly ground coffee — a small act of care for busy mornings and late nights.'
  },
  'home.freshCta': { zh: '浏览今日鲜食', en: 'Explore today’s fresh menu' },
  'home.freshNote': { zh: '当日现制 · 短保新鲜', en: 'Made today · Short shelf life' },
  'home.categoriesIndex': { zh: '02 / 货架', en: '02 / Shelves' },
  'home.categoriesTitle': { zh: '精心挑选的日常', en: 'An edited everyday' },
  'home.categoriesDesc': {
    zh: '从自有品牌到社区精选，我们把货架做得更短、更好。每一件商品，都值得被反复购买。',
    en: 'From private labels to neighborhood favourites, our shelves are shorter and better. Every item earns its place.'
  },
  'home.storesIndex': { zh: '03 / 门店', en: '03 / Stores' },
  'home.storesTitle': { zh: '就在你楼下，拐个弯就到', en: 'Downstairs, around the corner' },
  'home.storesDesc': {
    zh: '我们选择离生活最近的地方开店。步行可达的距离，熟悉的店员，和一杯不用排队的咖啡。',
    en: 'We open where life happens — a short walk away, with familiar faces and coffee without the queue.'
  },
  'home.storesCta': { zh: '查看全部门店', en: 'View all stores' },
  'home.franchiseIndex': { zh: '04 / 加盟', en: '04 / Franchise' },
  'home.franchiseTitle': { zh: '把一家好店，开进你的社区', en: 'Bring a good store to your neighborhood' },
  'home.franchiseDesc': {
    zh: '悦享便利提供从选址评估、门店设计、供应链到数字化运营的一体化支持。你负责热爱社区，我们负责把店开好。',
    en: 'From site assessment and store design to supply chain and digital operations, we support the whole journey. You bring the neighborhood; we bring the store.'
  },
  'home.franchiseCta': { zh: '了解加盟政策', en: 'Explore franchise' },
  'home.newsIndex': { zh: '05 / 资讯', en: '05 / Journal' },
  'home.newsTitle': { zh: '悦享正在发生', en: 'Happening at Yuexiang' },
  'home.newsCta': { zh: '阅读品牌资讯', en: 'Read the journal' },
  'home.bannerTitle': { zh: '社区 5 分钟生活圈', en: 'Your five-minute neighborhood' },

  'stores.eyebrow': { zh: '门店查询', en: 'Store locator' },
  'stores.title': { zh: '找到离你最近的一家店', en: 'Find your nearest store' },
  'stores.desc': {
    zh: '输入城区或关键词，查看门店地址、营业时间与配送范围。地图为示意定位，导航请使用高德地图。',
    en: 'Filter by district or keyword to see addresses, opening hours and delivery coverage. The map is indicative; directions open in Amap.'
  },
  'stores.city': { zh: '城市 / 城区', en: 'District' },
  'stores.allCities': { zh: '全部城区', en: 'All districts' },
  'stores.keyword': { zh: '关键字', en: 'Keyword' },
  'stores.keywordPlaceholder': { zh: '门店名称或地址', en: 'Store name or address' },
  'stores.result': { zh: '家门店', en: 'stores' },
  'stores.mapNote': { zh: '示意地图 · 非精确导航', en: 'Indicative map · not for navigation' },
  'stores.empty': { zh: '没有找到符合条件的门店', en: 'No stores match your filters' },
  'stores.reset': { zh: '清除筛选', en: 'Clear filters' },
  'stores.delivery': { zh: '支持配送', en: 'Delivery' },
  'stores.online': { zh: '线上营业', en: 'Online' },
  'stores.hours': { zh: '营业时间', en: 'Hours' },
  'stores.share': { zh: '门店分享页', en: 'Store page' },
  'stores.selected': { zh: '已选择', en: 'Selected' },
  'stores.pickHint': { zh: '点击地图上的定位点查看门店', en: 'Tap a pin to preview a store' },

  'products.eyebrow': { zh: '鲜食与商品', en: 'Fresh & goods' },
  'products.title': { zh: '今日鲜食，从清晨到深夜', en: 'Today’s fresh, from morning to midnight' },
  'products.desc': {
    zh: '只展示可在线选购的商品。价格、会员价与保质期均来自门店实时商品数据，下单请前往微信小程序。',
    en: 'Only items available online are shown. Prices, member prices and shelf life come from live store data. Order via our WeChat mini program.'
  },
  'products.search': { zh: '搜索商品', en: 'Search products' },
  'products.searchPlaceholder': { zh: '输入商品名称', en: 'Product name' },
  'products.all': { zh: '全部', en: 'All' },
  'products.member': { zh: '会员价', en: 'Member' },
  'products.miniTitle': { zh: '在微信小程序下单', en: 'Order in the WeChat mini program' },
  'products.miniDesc': {
    zh: '扫码进入线上商城，选择就近门店，支持到店自取与 30 分钟送达。',
    en: 'Scan to open the online store, choose your nearest branch, and enjoy pickup or 30-minute delivery.'
  },
  'products.miniNote': { zh: '微信搜索「悦享便利」小程序', en: 'Search “Yuexiang Mart” in WeChat' },
  'products.notForSale': { zh: '小程序有售', en: 'Available in mini program' },
  'products.empty': { zh: '没有找到相关商品', en: 'No products found' },

  'franchise.eyebrow': { zh: '加盟合作', en: 'Franchise' },
  'franchise.title': { zh: '和悦享一起，开一家社区好店', en: 'Build a neighborhood store with us' },
  'franchise.desc': {
    zh: '我们寻找认同社区价值的伙伴。不需要你是零售专家，但需要你愿意把一家店，认认真真地开在社区里。',
    en: 'We look for partners who believe in neighborhood. You do not need retail experience — you need the will to run a store with care.'
  },
  'franchise.conditionsTitle': { zh: '我们期待的你', en: 'Who we are looking for' },
  'franchise.condition1Title': { zh: '认同社区价值', en: 'Believe in neighborhood' },
  'franchise.condition1Desc': { zh: '愿意长期经营，把顾客当作邻居，而不是流量。', en: 'A long-term mindset — treating customers as neighbours, not traffic.' },
  'franchise.condition2Title': { zh: '合适的铺位', en: 'The right site' },
  'franchise.condition2Desc': { zh: '社区出入口、写字楼大堂或成熟街角，面积 40–120㎡。', en: 'Community entrances, office lobbies or established corners, 40–120 sqm.' },
  'franchise.condition3Title': { zh: '充足的资金', en: 'Sufficient capital' },
  'franchise.condition3Desc': { zh: '单店投资预算 35 万元起，具备 6 个月运营流动资金。', en: 'From RMB 350k per store, plus six months of working capital.' },
  'franchise.condition4Title': { zh: '全职投入', en: 'Full-time commitment' },
  'franchise.condition4Desc': { zh: '店主或核心合伙人全职参与门店日常经营。', en: 'The owner or a core partner is involved in daily operations full time.' },
  'franchise.feeTitle': { zh: '费用说明', en: 'Investment' },
  'franchise.feeNote': { zh: '以下为单店参考区间，最终以城市、店型与签约方案为准。', en: 'Reference ranges for a single store; final terms depend on city, format and agreement.' },
  'franchise.feeHeadItem': { zh: '项目', en: 'Item' },
  'franchise.feeHeadAmount': { zh: '参考金额', en: 'Reference' },
  'franchise.feeHeadNote': { zh: '说明', en: 'Notes' },
  'franchise.fee1': { zh: '加盟费', en: 'Franchise fee' },
  'franchise.fee1Amount': { zh: '3 万元 / 5 年', en: 'RMB 30k / 5 years' },
  'franchise.fee1Note': { zh: '品牌授权、选址评估与开业辅导', en: 'Brand licence, site assessment and opening support' },
  'franchise.fee2': { zh: '履约保证金', en: 'Performance deposit' },
  'franchise.fee2Amount': { zh: '2 万元', en: 'RMB 20k' },
  'franchise.fee2Note': { zh: '合同期满无违约全额退还', en: 'Refundable at contract end' },
  'franchise.fee3': { zh: '装修与设备', en: 'Fit-out & equipment' },
  'franchise.fee3Amount': { zh: '18–25 万元', en: 'RMB 180k–250k' },
  'franchise.fee3Note': { zh: '统一门店形象、冷链与收银设备', en: 'Store identity, cold chain and POS equipment' },
  'franchise.fee4': { zh: '首批货款', en: 'Opening inventory' },
  'franchise.fee4Amount': { zh: '8–12 万元', en: 'RMB 80k–120k' },
  'franchise.fee4Note': { zh: '按门店面积与商圈配置商品', en: 'Assorted by size and catchment' },
  'franchise.fee5': { zh: '流动资金', en: 'Working capital' },
  'franchise.fee5Amount': { zh: '建议 6 个月', en: '6 months advised' },
  'franchise.fee5Note': { zh: '覆盖租金、人力与日常周转', en: 'Rent, staffing and daily operations' },
  'franchise.supportTitle': { zh: '我们提供的支持', en: 'What we provide' },
  'franchise.support1': { zh: '选址评估', en: 'Site assessment' },
  'franchise.support1Desc': { zh: '客流测算、竞品分析与租约建议。', en: 'Footfall modelling, competitor analysis and lease advice.' },
  'franchise.support2': { zh: '供应链', en: 'Supply chain' },
  'franchise.support2Desc': { zh: '鲜食日配、短保管理与自有品牌商品。', en: 'Daily fresh delivery, short-dated management and private labels.' },
  'franchise.support3': { zh: '数字化系统', en: 'Digital systems' },
  'franchise.support3Desc': { zh: '收银、会员、库存与线上订单一体化。', en: 'POS, membership, inventory and online orders in one system.' },
  'franchise.support4': { zh: '门店培训', en: 'Store training' },
  'franchise.support4Desc': { zh: '开业驻店、鲜食操作与日常运营培训。', en: 'On-site opening support, fresh food handling and operations training.' },
  'franchise.formTitle': { zh: '提交加盟申请', en: 'Apply for a franchise' },
  'franchise.formDesc': { zh: '留下联系方式，加盟顾问将在 1–2 个工作日内与你联系。', en: 'Leave your details and a franchise advisor will contact you within 1–2 business days.' },
  'franchise.company': { zh: '公司 / 个体名称', en: 'Company / individual' },
  'franchise.contact': { zh: '联系人', en: 'Contact name' },
  'franchise.phone': { zh: '手机号码', en: 'Mobile number' },
  'franchise.city': { zh: '意向城市', en: 'Target city' },
  'franchise.amount': { zh: '投资预算', en: 'Investment budget' },
  'franchise.amountPlaceholder': { zh: '请选择预算区间', en: 'Select a range' },
  'franchise.amount1': { zh: '35 万元以下', en: 'Below RMB 350k' },
  'franchise.amount2': { zh: '35–50 万元', en: 'RMB 350k–500k' },
  'franchise.amount3': { zh: '50–80 万元', en: 'RMB 500k–800k' },
  'franchise.amount4': { zh: '80 万元以上', en: 'Above RMB 800k' },
  'franchise.remark': { zh: '补充说明', en: 'Additional notes' },
  'franchise.remarkPlaceholder': { zh: '铺位情况、计划开业时间等', en: 'Site details, planned opening date…' },
  'franchise.submit': { zh: '提交申请', en: 'Submit application' },
  'franchise.submitting': { zh: '提交中…', en: 'Submitting…' },
  'franchise.successTitle': { zh: '申请已收到', en: 'Application received' },
  'franchise.successDesc': { zh: '感谢你的信任。加盟顾问将在 1–2 个工作日内与你联系，请保持电话畅通。', en: 'Thank you. A franchise advisor will contact you within 1–2 business days.' },
  'franchise.again': { zh: '再提交一份', en: 'Submit another' },
  'franchise.required': { zh: '请填写此项', en: 'This field is required' },
  'franchise.phoneInvalid': { zh: '请输入有效的 11 位手机号码', en: 'Enter a valid 11-digit mobile number' },
  'franchise.privacy': { zh: '提交即表示同意我们仅为加盟咨询使用以上信息。', en: 'By submitting you agree that we use these details only for your franchise enquiry.' },
  'franchise.hotline': { zh: '加盟热线', en: 'Franchise hotline' },

  'news.eyebrow': { zh: '品牌资讯', en: 'Journal' },
  'news.title': { zh: '悦享正在发生', en: 'Happening at Yuexiang' },
  'news.desc': { zh: '门店、鲜食与社区的故事，以及最新的加盟动态。', en: 'Stories about our stores, fresh food and neighborhoods — plus the latest franchise news.' },
  'news.read': { zh: '阅读全文', en: 'Read story' },
  'news.back': { zh: '返回资讯列表', en: 'Back to journal' },
  'news.related': { zh: '继续阅读', en: 'Keep reading' },
  'news.empty': { zh: '暂无资讯', en: 'No stories yet' },
  'news.by': { zh: '撰文', en: 'By' },

  'contact.eyebrow': { zh: '联系我们', en: 'Contact' },
  'contact.title': { zh: '我们很想听见你的声音', en: 'We would love to hear from you' },
  'contact.desc': { zh: '无论是门店体验、商品建议还是加盟咨询，都欢迎联系我们。', en: 'Store experience, product ideas or franchise questions — we are here.' },
  'contact.hotline': { zh: '客服热线', en: 'Service hotline' },
  'contact.hotlineNote': { zh: '每日 07:00–23:00', en: 'Daily 07:00–23:00' },
  'contact.hours': { zh: '营业时间', en: 'Opening hours' },
  'contact.hoursNote': { zh: '各门店略有不同，以门店公示为准', en: 'Varies by store; see store details' },
  'contact.franchise': { zh: '加盟合作', en: 'Franchise' },
  'contact.franchiseNote': { zh: '提交申请，1–2 个工作日内回复', en: 'Apply and hear back within 1–2 business days' },
  'contact.storesTitle': { zh: '到店找我们', en: 'Visit a store' },
  'contact.messageTitle': { zh: '在线留言', en: 'Leave a message' },
  'contact.messageDesc': { zh: '留下你的联系方式与问题，我们会尽快回复。', en: 'Leave your contact and question; we will get back to you soon.' },
  'contact.name': { zh: '称呼', en: 'Your name' },
  'contact.phone': { zh: '手机号码', en: 'Mobile number' },
  'contact.message': { zh: '留言内容', en: 'Message' },
  'contact.submit': { zh: '发送留言', en: 'Send message' },
  'contact.success': { zh: '留言已发送，谢谢你。', en: 'Message sent. Thank you.' },
  'contact.messagePlaceholder': { zh: '想对我们说的话…', en: 'What would you like to tell us?' },

  'h5.navDeals': { zh: '折扣', en: 'Deals' },
  'h5.navNewcomer': { zh: '新人礼', en: 'Newcomer' },
  'h5.navFranchise': { zh: '加盟', en: 'Franchise' },
  'h5.navStores': { zh: '门店', en: 'Stores' },
  'h5.dealsTitle': { zh: '限时折扣', en: 'Limited-time deals' },
  'h5.dealsSub': { zh: '今日鲜食与好物，趁现在。', en: 'Today’s fresh food and finds — right now.' },
  'h5.promoEnds': { zh: '距结束', en: 'Ends in' },
  'h5.coupons': { zh: '先领券，再下单', en: 'Grab a coupon first' },
  'h5.claim': { zh: '立即领取', en: 'Claim' },
  'h5.claimTip': { zh: '请在微信小程序内领取', en: 'Claim inside the WeChat mini program' },
  'h5.newcomerTitle': { zh: '新人见面礼', en: 'A welcome gift' },
  'h5.newcomerSub': { zh: '第一次来，就该被好好招待。', en: 'Your first visit deserves a little treat.' },
  'h5.newcomerHero': { zh: '新人专享券包', en: 'Newcomer coupon pack' },
  'h5.newcomerDesc': { zh: '注册会员即可领取新人券，线上点单或到店核销。', en: 'Join as a member to receive newcomer coupons for online orders or in-store use.' },
  'h5.franchiseTitle': { zh: '开一家社区好店', en: 'Open a neighborhood store' },
  'h5.franchiseSub': { zh: '悦享便利加盟申请', en: 'Yuexiang Mart franchise application' },
  'h5.productInfo': { zh: '商品信息', en: 'Product details' },
  'h5.ingredients': { zh: '配料', en: 'Ingredients' },
  'h5.allergens': { zh: '致敏原', en: 'Allergens' },
  'h5.origin': { zh: '产地', en: 'Origin' },
  'h5.storage': { zh: '贮存', en: 'Storage' },
  'h5.shelf': { zh: '保质期', en: 'Shelf life' },
  'h5.openMini': { zh: '打开小程序购买', en: 'Buy in mini program' },
  'h5.orderTips': { zh: '微信搜索「悦享便利」小程序，选择就近门店下单。', en: 'Search “Yuexiang Mart” in WeChat and choose your nearest store.' },
  'h5.storeDelivery': { zh: '配送规则', en: 'Delivery' },
  'h5.minOrder': { zh: '起送金额', en: 'Minimum order' },
  'h5.deliveryFee': { zh: '配送费', en: 'Delivery fee' },
  'h5.freeThreshold': { zh: '免配送费', en: 'Free delivery over' },
  'h5.radius': { zh: '配送范围', en: 'Delivery radius' },
  'h5.timeSlots': { zh: '配送时段', en: 'Time slots' },
  'h5.payTitle': { zh: '支付结果', en: 'Payment result' },
  'h5.paySuccess': { zh: '支付已完成', en: 'Payment complete' },
  'h5.payPending': { zh: '未找到订单信息', en: 'Order information missing' },
  'h5.orderNo': { zh: '订单号', en: 'Order number' },
  'h5.viewOrder': { zh: '查看订单', en: 'View order' },
  'h5.backHome': { zh: '返回首页', en: 'Back to home' },
  'h5.payNote': { zh: '订单状态以小程序内显示为准。如已扣款但状态未更新，请稍后刷新或联系客服。', en: 'Order status is shown in the mini program. If payment is not reflected, refresh later or contact service.' },
  'h5.noDelivery': { zh: '该门店暂未配置配送规则', en: 'No delivery rule configured for this store' },
  'h5.shareCopied': { zh: '链接已复制', en: 'Link copied' },
  'h5.storeNotFound': { zh: '门店不存在或已关闭', en: 'Store not found or closed' },
  'h5.productNotFound': { zh: '商品不存在或已下架', en: 'Product not found or unavailable' }
}

export function useLang() {
  const lang = langRef
  const isEn = computed(() => lang.value === 'en')
  function setLang(next: Lang) {
    lang.value = next
    if (typeof localStorage !== 'undefined') localStorage.setItem('yx-lang', next)
    if (typeof document !== 'undefined') document.documentElement.lang = next === 'en' ? 'en' : 'zh-CN'
  }
  function t(key: string): string {
    const entry = dict[key]
    if (!entry) return key
    return entry[lang.value]
  }
  function menuTitle(menu: SiteMenu): string {
    if (lang.value === 'en' && menu.titleEn) return menu.titleEn
    return menu.title
  }
  function pick(zh?: string | null, en?: string | null): string {
    if (lang.value === 'en' && en) return en
    return zh || en || ''
  }
  return { lang, isEn, setLang, t, menuTitle, pick }
}

export const t = (key: string): string => {
  const entry = dict[key]
  if (!entry) return key
  return entry[langRef.value]
}
