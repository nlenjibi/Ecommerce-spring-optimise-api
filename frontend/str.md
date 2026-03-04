frontend/
├── 📁 .github/
│   ├── 📁 workflows/              # CI/CD pipelines
│   │   ├── 📄 ci.yml              # Continuous integration
│   │   ├── 📄 deploy.yml          # Deployment workflows
│   │   └── 📄 quality.yml         # Code quality checks
│   └── 📄 dependabot.yml          # Dependency updates
├── 📁 .husky/                     # Git hooks
│   ├── 📄 pre-commit             # Pre-commit checks
│   └── 📄 commit-msg             # Commit message validation
├── 📁 .next/                      # Next.js build output
├── 📁 public/                     # Static assets (CDN-ready)
│   ├── 🖼️ images/
│   │   ├── 📁 products/           # Product images (optimized)
│   │   ├── 📁 banners/            # Marketing banners
│   │   └── 📁 avatars/            # User avatars
│   ├── 🎨 icons/
│   │   ├── 📄 icon-192.png        # PWA icons
│   │   ├── 📄 icon-512.png
│   │   └── 📄 favicon.ico
│   ├── 📄 manifest.json           # PWA manifest
│   ├── 📄 robots.txt
│   └── 📄 sitemap.xml            # Generated sitemap
├── 📁 src/
│   ├── 📁 app/                    # Next.js 14+ App Router
│   │   ├── 📄 layout.tsx          # Root layout with providers
│   │   ├── 📄 template.tsx        # Reusable template
│   │   ├── 📄 loading.tsx         # Suspense fallback
│   │   ├── 📄 error.tsx           # Error boundary
│   │   ├── 📄 not-found.tsx       # 404 page
│   │   ├── 📄 globals.css         # Global styles
│   │   
│   │   ├── 📁 (marketing)/        # Marketing pages (public)
│   │   │   ├── 📄 page.tsx        # Homepage (SSR)
│   │   │   ├── 📁 about/
│   │   │   ├── 📁 contact/
│   │   │   ├── 📁 faq/
│   │   │   ├── 📁 deals/          # Dynamic with ISR
│   │   │   └── 📁 new-arrivals/   # Dynamic with ISR
│   │   ├── 📁 (shop)/             # Shopping experience
│   │   │   ├── 📁 products/       # SSG for product pages
│   │   │   │   ├── 📄 page.tsx    # Product listing (SSG)
│   │   │   │   ├── 📄 [slug]/     # Product detail (SSG)
│   │   │   │   └── 📁 categories/ # Static categories
│   │   │   ├── 📁 search/         # Client-side search
│   │   │   ├── 📁 cart/           # Dynamic cart
│   │   │   └── 📁 wishlist/       # Dynamic wishlist
│   │   ├── 📁 (auth)/             # Authentication flow
│   │   │   ├── 📁 login/          # SSR for auth pages
│   │   │   ├── 📁 register/
│   │   │   ├── 📁 forgot-password/
│   │   │   └── 📁 reset-password/
│   │   ├── 📁 (dashboard)/        # Protected dashboard routes
│   │   │   ├── 📄 layout.tsx      # Dashboard layout
│   │   │   ├── 📁 admin/          # Role-based access
│   │   │   ├── 📁 seller/
│   │   │   ├── 📁 customer/
│   │   │   └── 📁 settings/       # Shared settings
│   │   └── 📁 (legal)/            # Legal pages (static)
│   ├── 📁 components/             # Component library
│   │   ├── 📁 ui/                 # Design system (shadcn/ui inspired)
│   │   │   ├── 📄 button.tsx      # With variants and styles
│   │   │   ├── 📄 input.tsx
│   │   │   ├── 📄 card.tsx
│   │   │   ├── 📄 dialog.tsx
│   │   │   ├── 📄 skeleton.tsx    # Loading states
│   │   │   ├── 📄 sheet.tsx       # Mobile drawers
│   │   │   └── 📄 index.ts        # Barrel exports
│   │   ├── 📁 layout/             # Layout components
│   │   │   ├── 📄 header.tsx      # With mobile responsive
│   │   │   ├── 📄 footer.tsx
│   │   │   ├── 📄 sidebar.tsx     # Collapsible
│   │   │   └── 📄 navigation/     # Organized nav components
│   │   ├── 📁 forms/              # React Hook Form integration
│   │   │   ├── 📁 schemas/        # Zod validation schemas
│   │   │   ├── 📄 form.tsx        # Base form component
│   │   │   ├── 📄 form-field.tsx  # Controlled field
│   │   │   └── 📁 hooks/          # Form-specific hooks
│   │   ├── 📁 features/           # Feature-based components
│   │   │   ├── 📁 auth/           # Auth-related components
│   │   │   ├── 📁 product/        # Product features
│   │   │   ├── 📁 cart/           # Cart features
│   │   │   ├── 📁 checkout/       # Checkout flow
│   │   │   ├── 📁 dashboard/      # Dashboard features
│   │   │   └── 📁 search/         # Search components
│   │   ├── 📁 shared/             # Cross-feature components
│   │   │   ├── 📄 error-boundary.tsx
│   │   │   ├── 📄 seo.tsx         # Next.js metadata
│   │   │   ├── 📄 image.tsx       # Optimized image wrapper
│   │   │   └── 📄 suspense.tsx    # Loading wrapper
│   │   └── 📁 providers/          # Context providers as components
│   ├── 📁 hooks/                  # Custom hooks library
│   │   ├── 📁 api/                # Data fetching hooks
│   │   │   ├── 📄 use-query.ts    # React Query integration
│   │   │   ├── 📄 use-mutation.ts
│   │   │   └── 📄 use-infinite-query.ts
│   │   ├── 📁 state/              # State management hooks
│   │   │   ├── 📄 use-local-storage.ts
│   │   │   ├── 📄 use-session-storage.ts
│   │   │   └── 📄 use-toggle.ts
│   │   ├── 📁 ui/                 # UI interaction hooks
│   │   │   ├── 📄 use-debounce.ts
│   │   │   ├── 📄 use-intersection-observer.ts
│   │   │   ├── 📄 use-keypress.ts
│   │   │   └── 📄 use-media-query.ts
│   │   ├── 📁 domain/             # Business logic hooks
│   │   │   ├── 📄 use-auth.ts
│   │   │   ├── 📄 use-cart.ts
│   │   │   ├── 📄 use-products.ts
│   │   │   └── 📄 use-orders.ts
│   │   └── 📄 index.ts            # Barrel exports
│   ├── 📁 lib/                    # Core infrastructure
│   │   ├── 📁 api/                # HTTP client layer
│   │   │   ├── 📄 client.ts       # Axios/fetch wrapper
│   │   │   ├── 📄 interceptors.ts  # Auth & error handling
│   │   │   ├── 📄 endpoints/      # Organized API endpoints
│   │   │   │   ├── 📄 auth.endpoints.ts
│   │   │   │   ├── 📄 products.endpoints.ts
│   │   │   │   └── 📄 orders.endpoints.ts
│   │   │   └── 📄 cache/          # React Query config
│   │   ├── 📁 utils/              # Pure utilities
│   │   │   ├── 📄 format.ts       # Number, date formatting
│   │   │   ├── 📄 validation.ts   # Zod schemas
│   │   │   ├── 📄 price.ts        # Price calculations
│   │   │   ├── 📄 storage.ts      # Storage abstraction
│   │   │   ├── 📄 error.ts        # Error handling
│   │   │   └── 📄 performance.ts  # Performance utils
│   │   ├── 📁 constants/          # App constants
│   │   │   ├── 📄 routes.ts       # Path constants
│   │   │   ├── 📄 roles.ts        # User roles & permissions
│   │   │   ├── 📄 breakpoints.ts  # Responsive design
│   │   │   └── 📄 seo.ts          # SEO constants
│   │   ├── 📁 validators/         # Validation schemas
│   │   │   ├── 📄 auth.schema.ts
│   │   │   ├── 📄 product.schema.ts
│   │   │   └── 📄 order.schema.ts
│   │   └── 📁 services/           # External services
│   │       ├── 📄 analytics.ts     # Plausible/Google Analytics
│   │       ├── 📄 payment.ts       # Stripe integration
│   │       ├── 📄 email.ts         # Email service
│   │       └── 📄 tracking.ts      # Mixpanel/FullStory
│   ├── 📁 store/                  # State management (Zustand)
│   │   ├── 📁 slices/              # Feature slices
│   │   │   ├── 📄 auth.store.ts
│   │   │   ├── 📄 cart.store.ts
│   │   │   ├── 📄 ui.store.ts
│   │   │   └── 📄 notifications.store.ts
│   │   ├── 📄 index.ts             # Combined store
│   │   └── 📄 middleware.ts        # Store middleware
│   ├── 📁 types/                  # TypeScript definitions
│   │   ├── 📄 index.ts            # Main exports
│   │   ├── 📄 api.ts              # API types
│   │   ├── 📄 database.ts         # Database models
│   │   ├── 📄 forms.ts            # Form types
│   │   └── 📄 next.ts             # Next.js specific types
│   ├── 📁 styles/                 # Styling system
│   │   ├── 📄 globals.css         # CSS variables & reset
│   │   ├── 📄 components.css      # Component styles
│   │   ├── 📄 utils.css           # Utility classes
│   │   └── 📁 themes/             # Multiple themes
│   └── 📁 middleware/             # Custom middleware
│       ├── 📄 auth.ts              # Authentication middleware
│       ├── 📄 rate-limit.ts       # Rate limiting
│       └── 📄 logging.ts          # Request logging
├── 📁 tests/                      # Testing setup
│   ├── 📁 unit/                   # Unit tests
│   ├── 📁 integration/            # Integration tests
│   ├── 📁 e2e/                    # End-to-end tests
│   ├── 📄 setup.ts                # Test setup
│   └── 📄 jest.config.ts          # Jest configuration
├── 📁 docs/                       # Project documentation
│   ├── 📄 architecture.md         # Architecture decisions
│   ├── 📄 components.md           # Component guidelines
│   └── 📄 api.md                  # API documentation
├── 📄 .env.example                # Environment template
├── 📄 .env.local                  # Local environment
├── 📄 .env.production             # Production environment
├── 📄 next.config.ts              # Next.js config (TypeScript)
├── 📄 tailwind.config.ts          # Tailwind configuration
├── 📄 tsconfig.json               # TypeScript config
├── 📄 biome.json                  # Linting & formatting
├── 📄 package.json                # Dependencies
└── 📄 README.md                   # Project overview
