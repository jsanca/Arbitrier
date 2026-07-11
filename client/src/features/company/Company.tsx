import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Company, User } from '../../models/types';
import { services } from '../../services/mockServices';
import { CreditSummary } from '../../components/CreditSummary';
import { LoadingState } from '../../components/LoadingState';

export const CompanyPage: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [company, setCompany] = useState<Company | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const loadCompanyData = async () => {
      try {
        const currentUser = await services.auth.getCurrentUser();
        if (!currentUser) {
          navigate('/login');
          return;
        }
        setUser(currentUser);

        const currentCompany = await services.company.getCompany(currentUser.companyId);
        setCompany(currentCompany);
      } catch (err) {
        console.error('Failed to load company details', err);
      } finally {
        setLoading(false);
      }
    };

    loadCompanyData();
  }, [navigate]);

  if (loading) return <LoadingState message="Fetching corporate profiles..." />;
  if (!user || !company) return null;

  return (
    <div className="space-y-8" data-testid="company-page">
      <div>
        <h1 className="text-2xl font-bold font-headline-lg text-on-surface">Company Profile</h1>
        <p className="text-sm text-on-surface-variant mt-1">
          Review corporate account permissions, credit balances, and billing logistics.
        </p>
      </div>

      {/* Credit balance banner */}
      <CreditSummary availableCredit={company.availableCredit} creditLimit={company.creditLimit} />

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Left Side: General Profile & Billing */}
        <div className="space-y-6">
          {/* General Metadata */}
          <div className="bg-surface-container-lowest border border-outline-variant p-6 rounded-xl shadow-sm space-y-4">
            <h3 className="text-sm font-bold font-headline-md border-b border-outline-variant pb-2 flex items-center gap-1.5">
              <span className="material-symbols-outlined text-[18px] text-primary">domain</span>
              Corporate Profile Info
            </h3>
            <div className="grid grid-cols-2 gap-y-3 gap-x-4 text-xs">
              <div>
                <p className="text-secondary font-medium uppercase tracking-wider">Legal Entity Name</p>
                <p className="font-semibold text-on-surface text-sm mt-0.5">{company.name}</p>
              </div>
              <div>
                <p className="text-secondary font-medium uppercase tracking-wider">Account ID</p>
                <p className="font-mono text-on-surface mt-0.5">{company.id}</p>
              </div>
              <div>
                <p className="text-secondary font-medium uppercase tracking-wider">Payment Terms</p>
                <p className="font-semibold text-on-surface mt-0.5">{company.paymentTerms}</p>
              </div>
              <div>
                <p className="text-secondary font-medium uppercase tracking-wider">Billing Method</p>
                <p className="font-semibold text-on-surface mt-0.5">Corporate Net Invoicing</p>
              </div>
            </div>
          </div>

          {/* Billing Contact */}
          <div className="bg-surface-container-lowest border border-outline-variant p-6 rounded-xl shadow-sm space-y-4">
            <h3 className="text-sm font-bold font-headline-md border-b border-outline-variant pb-2 flex items-center gap-1.5">
              <span className="material-symbols-outlined text-[18px] text-primary">receipt_long</span>
              Billing Department Contact
            </h3>
            <div className="space-y-2 text-xs">
              <div className="flex justify-between">
                <span className="text-secondary">Primary Contact Person:</span>
                <span className="font-semibold text-on-surface">{company.billingContact.name}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-secondary">Accounts Payable Email:</span>
                <span className="font-mono text-on-surface">{company.billingContact.email}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-secondary">Direct Telephone:</span>
                <span className="font-semibold text-on-surface">{company.billingContact.phone}</span>
              </div>
            </div>
          </div>

          {/* Authorized Buyers */}
          <div className="bg-surface-container-lowest border border-outline-variant p-6 rounded-xl shadow-sm space-y-3">
            <h3 className="text-sm font-bold font-headline-md border-b border-outline-variant pb-2 flex items-center gap-1.5">
              <span className="material-symbols-outlined text-[18px] text-primary">group</span>
              Authorized Corporate Buyers
            </h3>
            <div className="divide-y divide-outline-variant/40">
              {company.authorizedBuyers.map((buyer, idx) => (
                <div key={idx} className="py-2.5 flex items-center justify-between text-xs">
                  <span className="font-medium text-on-surface">{buyer}</span>
                  <span className="text-[10px] bg-primary-container text-on-primary-container px-2 py-0.5 rounded uppercase font-semibold">
                    Procurement Officer
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right Side: Shipping destinations */}
        <div className="bg-surface-container-lowest border border-outline-variant p-6 rounded-xl shadow-sm space-y-4">
          <h3 className="text-sm font-bold font-headline-md border-b border-outline-variant pb-2 flex items-center gap-1.5">
            <span className="material-symbols-outlined text-[18px] text-primary">local_shipping</span>
            Registered Shipping Destinations
          </h3>

          <div className="space-y-4">
            {company.shippingAddresses.map((addr, idx) => (
              <div key={addr.id} className="p-4 border border-outline-variant rounded-lg bg-surface-container-low/30 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-primary flex items-center gap-1">
                    <span className="material-symbols-outlined text-[16px]">location_on</span>
                    {addr.name}
                  </span>
                  {idx === 0 && (
                    <span className="text-[9px] bg-emerald-100 text-emerald-800 font-semibold px-2 py-0.5 rounded uppercase">
                      Primary
                    </span>
                  )}
                </div>
                <div className="text-xs text-on-surface-variant space-y-0.5 pl-5">
                  <p className="font-semibold text-on-surface">{addr.street}</p>
                  <p>
                    {addr.city}, {addr.state} {addr.zip}
                  </p>
                  <p>{addr.country}</p>
                </div>
              </div>
            ))}
          </div>

          <div className="p-3.5 bg-surface-container border border-outline-variant/60 rounded-xl text-center text-xs text-on-surface-variant mt-4">
            Shipping destinations are managed via administrative profile controls. Contact your system admin to register new warehouse endpoints.
          </div>
        </div>
      </div>
    </div>
  );
};
