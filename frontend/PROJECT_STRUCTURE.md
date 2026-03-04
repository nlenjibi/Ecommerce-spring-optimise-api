# E-commerce Frontend Project Structure

This is a Next.js 14 e-commerce frontend application with TypeScript, Tailwind CSS, and modern React patterns.

## 📁 Root Directory Structure

```
frontend/
├── .env                          # Environment variables (local)
├── .env.example                  # Environment variables template
├── .env.local                    # Local environment overrides
├── .env.production               # Production environment variables
├── .gitignore                    # Git ignore rules
├── .github/                      # GitHub workflows
├── .idea/                        # IDE configuration
├── .next/                        # Next.js build output
├── node_modules/                 # Node.js dependencies
├── public/                       # Static assets
├── src/                          # Source code
├── formatted_public.json         # Formatted public data
├── next-env.d.ts                 # Next.js TypeScript definitions
├── next.config.js                # Next.js configuration
├── package-lock.json             # Dependency lock file
├── package.json                  # Project metadata and scripts
├── postcss.config.js             # PostCSS configuration
├── tailwind.config.ts            # Tailwind CSS configuration
├── tsconfig.json                 # TypeScript configuration
├── tsconfig.tsbuildinfo          # TypeScript build cache
└── str.md                        # Documentation
```

## 📂 Source Code Structure (`src/`)

```
src/
├── app/                          # Next.js App Router pages
├── components/                   # React components
├── context/                      # React context providers
├── hooks/                        # Custom React hooks
├── lib/                          # Utility functions and configurations
├── middleware/                   # Next.js middleware
├── store/                        # State management (Zustand)
├── styles/                       # Global styles and CSS
└── types/                        # TypeScript type definitions
```

## 📄 App Router Structure (`src/app/`)

```
app/
├── auth/                         # Authentication pages
├── dashboard/                    # Dashboard pages
├── marketing/                    # Marketing/landing pages
├── shop/                         # E-commerce shop pages
├── unauthorized/                 # Unauthorized access page
├── error.tsx                     # Error boundary component
├── globals.css                   # Global CSS styles
├── layout.tsx                    # Root layout component
├── not-found.tsx                 # 404 page
└── page.tsx                      # Home page
```

## 🧩 Components Structure (`src/components/`)

```
components/
├── features/                     # Feature-specific components
├── filters/                      # Filter components
├── forms/                        # Form components
├── layout/                       # Layout components
├── providers/                    # Context providers
├── shared/                       # Shared/common components
├── skeletons/                    # Loading skeleton components
├── ui/                           # UI component library
└── index.ts                      # Component exports
```

## 🛠️ Technology Stack

### Core Framework
- **Next.js 14** - React framework with App Router
- **React 18** - UI library
- **TypeScript** - Type safety

### Styling
- **Tailwind CSS** - Utility-first CSS framework
- **PostCSS** - CSS post-processor

### State Management & Data Fetching
- **Zustand** - Lightweight state management
- **TanStack Query** - Server state management
- **Axios** - HTTP client

### UI & UX
- **Lucide React** - Icon library
- **React Hot Toast** - Toast notifications
- **Recharts** - Chart library

### Payments
- **Stripe** - Payment processing

### Utilities
- **js-cookie** - Cookie management
- **date-fns** - Date manipulation

### Development Tools
- **ESLint** - Code linting
- **TypeScript** - Type checking

## 🚀 Available Scripts

```json
{
  "dev": "next dev",           # Start development server
  "build": "next build",       # Build for production
  "start": "next start",       # Start production server
  "lint": "next lint"          # Run ESLint
}
```

## 📝 Key Features

- **E-commerce functionality** with shop pages
- **User authentication** and authorization
- **Dashboard** for admin/user management
- **Payment integration** with Stripe
- **Responsive design** with Tailwind CSS
- **Type safety** with TypeScript
- **Modern React patterns** with hooks and context
- **Server-side rendering** with Next.js App Router
- **State management** with Zustand
- **Data fetching** with TanStack Query

## 🔧 Environment Configuration

The project uses multiple environment files for different environments:
- `.env` - Local development
- `.env.local` - Local overrides
- `.env.production` - Production settings
- `.env.example` - Template for new environments

## 📦 Dependencies

### Production Dependencies
- Core React and Next.js packages
- UI libraries (Tailwind, Lucide)
- State management (Zustand, TanStack Query)
- Payment processing (Stripe)
- Utilities (Axios, date-fns, js-cookie)

### Development Dependencies
- TypeScript and type definitions
- Build tools (PostCSS, Tailwind)
- Code quality tools (ESLint)
