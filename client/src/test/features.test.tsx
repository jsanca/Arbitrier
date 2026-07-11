import { describe, test, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { Login } from '../features/auth/Login';
import { NewOrder } from '../features/orders/NewOrder';
import { AvailabilityReview } from '../features/orders/AvailabilityReview';
import { OrdersList } from '../features/orders/OrdersList';
import { services, initDB } from '../services/mockServices';

// Clear localStorage and reset state before each test
beforeEach(() => {
  localStorage.clear();
  initDB();
});

describe('Login Flow', () => {
  test('successful mock login redirects to dashboard', async () => {
    const loginSpy = vi.spyOn(services.auth, 'login');

    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/dashboard" element={<div>Dashboard Mock Page</div>} />
        </Routes>
      </MemoryRouter>
    );

    const emailInput = screen.getByLabelText(/Email Address/i);
    const submitBtn = screen.getByRole('button', { name: 'Sign In' });

    fireEvent.change(emailInput, { target: { value: 'test@company.com' } });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(loginSpy).toHaveBeenCalledWith('test@company.com', 'password');
      expect(screen.getByText('Dashboard Mock Page')).toBeInTheDocument();
    });
  });

  test('sso login redirects to dashboard', async () => {
    const loginSpy = vi.spyOn(services.auth, 'login');

    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/dashboard" element={<div>Dashboard Mock Page</div>} />
        </Routes>
      </MemoryRouter>
    );

    const ssoBtn = screen.getByRole('button', { name: /Sign in with Keycloak SSO/i });
    fireEvent.click(ssoBtn);

    await waitFor(() => {
      expect(loginSpy).toHaveBeenCalledWith('sso-buyer@arbitrier.com');
      expect(screen.getByText('Dashboard Mock Page')).toBeInTheDocument();
    });
  });
});

describe('New Order Cart modification', () => {
  test('allows searching, adding products, and saving drafts', async () => {
    render(
      <MemoryRouter initialEntries={['/orders/new']}>
        <NewOrder />
      </MemoryRouter>
    );

    // Verify loader finishes
    await waitFor(() => {
      expect(screen.getByText('Product Catalog')).toBeInTheDocument();
    });

    // We have 5 default items. Find "Industrial Hydraulic Hose" quick add.
    const addHoseBtn = screen.getAllByRole('button', { name: /Add/i })[0];
    fireEvent.click(addHoseBtn);

    // Verify item is added to draft cart
    await waitFor(() => {
      expect(screen.getByText('Draft Item List')).toBeInTheDocument();
      expect(screen.getByText('1 unique products')).toBeInTheDocument();
    });

    // Check quantity increment button - draft starts with qty=3, after add becomes 4, after increment becomes 5
    const incBtn = screen.getByLabelText('Increase quantity');
    fireEvent.click(incBtn);

    await waitFor(() => {
      expect(screen.getByText('5')).toBeInTheDocument(); // Quantity becomes 5
    });
  });
});

describe('Availability Review stock negotiations', () => {
  test('resolving stock levels and confirming submittal', async () => {
    // Populate draft order so review page loads
    const user = await services.auth.login('brio@arbitrier.com');
    const draft = await services.preparation.getDraftOrder(user.companyId, user.id);
    // Add prod-3 (Pump) in quantity > 2 (triggers partial availability review)
    draft.items = [{
      productId: 'prod-3',
      productName: 'Heavy-Duty Rotary Pump',
      sku: 'SKU-PMP-772',
      price: 1250,
      requestedQuantity: 5,
      availableQuantity: 2,
      acceptedQuantity: 5
    }];
    await services.preparation.saveDraftOrder(draft);

    render(
      <MemoryRouter initialEntries={[`/orders/review?orderId=${draft.id}`]}>
        <Routes>
          <Route path="/orders/review" element={<AvailabilityReview />} />
          <Route path="/orders/submitting" element={<div>Submitting Loader Page</div>} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Availability Review')).toBeInTheDocument();
      expect(screen.getByText('Heavy-Duty Rotary Pump')).toBeInTheDocument();
      expect(screen.getByText('Partially Available')).toBeInTheDocument();
    });

    // Click Accept Available
    const acceptAvailableBtn = screen.getByRole('button', { name: /Accept Available/i });
    fireEvent.click(acceptAvailableBtn);

    const submitBtn = screen.getByRole('button', { name: /Confirm & Submit Purchase/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Submitting Loader Page')).toBeInTheDocument();
    });
  });
});

describe('Orders List Filtering', () => {
  test('filtering records based on search and status tabs', async () => {
    // Ensure mock orders exist
    await services.auth.login('brio@arbitrier.com');

    render(
      <MemoryRouter initialEntries={['/orders']}>
        <OrdersList />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Procurement Orders')).toBeInTheDocument();
    });

    // Filter by Drafts
    const draftTab = screen.getByRole('button', { name: 'Drafts' });
    fireEvent.click(draftTab);

    // Check query filter matches
    const searchInput = screen.getByPlaceholderText(/Search by Reference ID/i);
    fireEvent.change(searchInput, { target: { value: 'NonExistentRef' } });

    await waitFor(() => {
      expect(screen.getByText('No orders match filters')).toBeInTheDocument();
    });
  });
});
