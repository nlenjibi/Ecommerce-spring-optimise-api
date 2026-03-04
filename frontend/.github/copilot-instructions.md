# Next.js eCommerce Application

## Project Overview
This is a full-featured eCommerce application built with Next.js 14, TypeScript, and Tailwind CSS.

## Tech Stack
- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **State Management**: React Context API

## Project Structure
```
frontend/
├── app/                    # Next.js App Router pages
│   ├── page.tsx           # Home page
│   ├── layout.tsx         # Root layout
│   ├── products/          # Product pages
│   ├── cart/              # Cart page
│   ├── checkout/          # Checkout page
│   ├── login/             # Authentication pages
│   ├── dashboard/         # User dashboard
│   └── admin/             # Admin dashboard
├── components/            # Reusable UI components
├── context/               # React Context providers
├── services/              # API service layer
├── types/                 # TypeScript interfaces
└── public/                # Static assets
```

## Development Guidelines
- Use TypeScript for all new files
- Follow the existing component patterns
- Use Tailwind CSS utility classes for styling
- Maintain consistent code formatting

## Available Scripts
- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run start` - Start production server
- `npm run lint` - Run ESLint
