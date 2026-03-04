# Cart "Add to Cart" Bug Fix

## Issue Description
Users were unable to add items to cart even when logged in from the frontend. The tracking events were firing correctly, but users were seeing a "Please log in to add items to your cart" message despite being authenticated.

## Root Causes
There were **TWO critical bugs** in [`src/context/CartContext.tsx`](src/context/CartContext.tsx):

### Bug #1: Cart Initialization (lines 246-271)

#### The Problem
```typescript
const initializeCart = useCallback(async () => {
  try {
    setLoading(true);

    // Only load cart if user is authenticated
    if (isAuthenticated()) {
      const username = getUsername();
      try {
        const cartData = await apiCall(`/v1/carts/me?username=${encodeURIComponent(username!)}`);
        setCart(cartData);  // ✅ Cart loaded successfully
      } catch (error: any) {
        console.log('No existing user cart');
      }
    }

    setCart(null);  // ❌ BUG: This line unconditionally clears the cart!
  } catch (error: any) {
    // error handling...
  } finally {
    setLoading(false);
  }
}, []);
```

**The issue:** Line 262 had `setCart(null)` being called unconditionally, which meant that even after successfully loading the cart data, it would immediately be cleared. This prevented the cart from being initialized properly.

### Bug #2: Authentication Check (lines 160-171)

#### The Problem
```typescript
const USERNAME_STORAGE_KEY = 'username';

const getUsername = (): string | null => {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(USERNAME_STORAGE_KEY);  // ❌ BUG: 'username' key doesn't exist!
};

const isAuthenticated = (): boolean => {
  return isAuthenticatedUtil() && !!getUsername();  // ❌ Always returns false!
};
```

**The issue:** The `getUsername()` function was looking for a `'username'` key in localStorage, but the AuthContext stores the user object under `'user'` or `'user_data'` keys. This meant `getUsername()` always returned `null`, causing `isAuthenticated()` to always return `false`, even when the user was logged in.

## The Fixes

### Fix #1: Cart Initialization
Moved `setCart(null)` inside the catch block and added proper handling for unauthenticated users:

```typescript
const initializeCart = useCallback(async () => {
  try {
    setLoading(true);

    // Only load cart if user is authenticated
    if (isAuthenticated()) {
      const username = getUsername();
      try {
        const cartData = await apiCall(`/v1/carts/me?username=${encodeURIComponent(username!)}`);
        setCart(cartData);  // ✅ Cart loaded successfully
      } catch (error: any) {
        // No cart exists yet, will be created when first item is added
        console.log('No existing user cart');
        setCart(null);  // ✅ Only clear cart if it doesn't exist
      }
    } else {
      // User is not authenticated, clear cart
      setCart(null);  // ✅ Clear cart for unauthenticated users
    }
  } catch (error: any) {
    console.error('Failed to initialize cart:', error);
    if (error.message !== 'Authentication required. Please log in to continue.') {
      toast.error('Failed to load cart');
    }
  } finally {
    setLoading(false);
  }
}, []);
```

### Fix #2: Authentication Check
Updated `getUsername()` to read from the correct localStorage keys and simplified `isAuthenticated()`:

```typescript
const getUsername = (): string | null => {
  if (typeof window === 'undefined') return null;
  
  // Try to get user from localStorage (stored by AuthContext)
  try {
    const userStr = localStorage.getItem('user') || localStorage.getItem('user_data');
    if (userStr) {
      const user = JSON.parse(userStr);
      // Return email as username (backend accepts both)
      return user.email || user.username || null;
    }
  } catch (e) {
    console.error('Failed to parse user from localStorage:', e);
  }
  
  return null;
};

const isAuthenticated = (): boolean => {
  return isAuthenticatedUtil();  // ✅ Just check for token, username is retrieved separately
};
```

### Additional Fix
Fixed TypeScript errors in the `computedValues` useMemo (lines 758-769) by removing references to non-existent properties:
- Removed `cart?.totalAmount` fallback (property doesn't exist on Cart interface)
- Removed `cart?.discountAmount` fallback (property doesn't exist on Cart interface)

## Impact
These fixes ensure that:
1. ✅ Authenticated users can successfully add items to their cart
2. ✅ Cart data persists after initialization
3. ✅ Cart is properly cleared for unauthenticated users
4. ✅ New carts are created when needed (on first add to cart)
5. ✅ Authentication state is correctly detected from localStorage
6. ✅ Username/email is properly extracted from the user object
7. ✅ TypeScript errors are resolved

## Testing Recommendations
1. **Test as authenticated user:**
   - Log in to the application
   - Navigate to a product page
   - Click "Add to Cart"
   - Verify item is added successfully (no "login" message)
   - Check cart page to see items
   - Verify success toast appears

2. **Test cart persistence:**
   - Add items to cart
   - Refresh the page
   - Verify cart items are still present

3. **Test as unauthenticated user:**
   - Log out
   - Try to add items to cart
   - Verify appropriate error message is shown

4. **Test cart creation:**
   - Log in with a new user (no existing cart)
   - Add first item to cart
   - Verify cart is created automatically

5. **Test tracking events:**
   - Open browser console
   - Add items to cart
   - Verify tracking events are logged
   - Verify no authentication errors in console

## Files Modified
- [`src/context/CartContext.tsx`](src/context/CartContext.tsx) - Fixed `initializeCart` function, `getUsername` function, `isAuthenticated` function, and TypeScript errors

## Related Components
The following components use the `addToCart` function and will benefit from this fix:
- [`src/app/shop/products/[slug]/page.tsx`](src/app/shop/products/[slug]/page.tsx) - Product detail page
- [`src/components/features/products/ProductCard.tsx`](src/components/features/products/ProductCard.tsx) - Product cards
- [`src/components/features/cart/CartFromTrackingButton.tsx`](src/components/features/cart/CartFromTrackingButton.tsx) - Cart tracking
- [`src/context/WishlistContext.tsx`](src/context/WishlistContext.tsx) - Wishlist to cart functionality

## Technical Details

### Why the username check was failing:
1. **AuthContext** stores user data as: `localStorage.setItem('user', JSON.stringify(userObject))`
2. **CartContext** was looking for: `localStorage.getItem('username')`
3. These keys don't match, so authentication always failed

### Why we use email as username:
The backend cart API accepts `username` as a parameter, but it can handle both username and email. Since the user object always has an email (required field), we use that as the identifier for cart operations.
