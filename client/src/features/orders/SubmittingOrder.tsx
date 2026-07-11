import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import type { Order } from '../../models/types';
import { services } from '../../services/mockServices';

export const SubmittingOrder: React.FC = () => {
  const [searchParams] = useSearchParams();
  const orderId = searchParams.get('orderId') || '';
  const navigate = useNavigate();

  const [order, setOrder] = useState<Order | null>(null);
  const [currentStep, setCurrentStep] = useState(0);

  const steps = [
    { label: 'Checking product availability', desc: 'Verifying real-time warehouse stock allocations.' },
    { label: 'Validating company credit', desc: 'Evaluating transaction values against available account limit.' },
    { label: 'Preparing confirmation', desc: 'Finalizing procurement contracts and delivery itinerary.' }
  ];

  useEffect(() => {
    const loadOrder = async () => {
      if (!orderId) {
        navigate('/dashboard');
        return;
      }
      const draft = await services.order.getOrderById(orderId);
      if (!draft) {
        navigate('/dashboard');
        return;
      }
      setOrder(draft);
    };

    loadOrder();
  }, [orderId, navigate]);

  useEffect(() => {
    if (!order) return;

    const interval = setInterval(() => {
      setCurrentStep(prev => {
        if (prev < steps.length - 1) {
          return prev + 1;
        } else {
          clearInterval(interval);
          // Trigger the submission in the service
          services.order.submitOrder(order).then(submitted => {
            navigate(`/orders/outcome?orderId=${submitted.id}`);
          });
          return prev;
        }
      });
    }, 1500);

    return () => clearInterval(interval);
  }, [order, navigate, steps.length]);

  if (!order) return null;

  return (
    <div className="min-h-[60vh] flex flex-col items-center justify-center py-12 px-4 sm:px-6 lg:px-8 text-center" data-testid="submitting-order-page">
      <div className="max-w-md w-full space-y-8 bg-surface-container-lowest border border-outline-variant p-8 rounded-2xl shadow-sm">
        {/* Animated loader */}
        <div className="relative w-16 h-16 mx-auto">
          <div className="absolute inset-0 rounded-full border-4 border-surface-container-high"></div>
          <div className="absolute inset-0 rounded-full border-4 border-primary border-t-transparent animate-spin"></div>
        </div>

        <div>
          <h2 className="text-xl font-bold text-on-surface">Submitting Procurement Purchase</h2>
          <p className="text-xs text-on-surface-variant mt-2">
            Order Reference: <span className="font-mono font-semibold">{orderId}</span>
          </p>
        </div>

        {/* Milestones */}
        <div className="space-y-4 text-left border-t border-b border-outline-variant/40 py-6 my-4">
          {steps.map((step, idx) => {
            const isCompleted = idx < currentStep;
            const isActive = idx === currentStep;

            let icon = 'radio_button_unchecked';
            let iconClass = 'text-secondary opacity-40';
            let textClass = 'text-secondary/70';

            if (isCompleted) {
              icon = 'check_circle';
              iconClass = 'text-emerald-500';
              textClass = 'text-on-surface font-medium';
            } else if (isActive) {
              icon = 'sync';
              iconClass = 'text-primary animate-spin';
              textClass = 'text-primary font-semibold';
            }

            return (
              <div key={idx} className="flex gap-3">
                <span className={`material-symbols-outlined text-[20px] shrink-0 mt-0.5 ${iconClass}`}>
                  {icon}
                </span>
                <div>
                  <h4 className={`text-xs uppercase tracking-wider ${textClass}`}>{step.label}</h4>
                  {isActive && <p className="text-[11px] text-on-surface-variant mt-0.5">{step.desc}</p>}
                </div>
              </div>
            );
          })}
        </div>

        {/* Non-blocking Notice */}
        <div className="bg-surface-container border border-outline-variant/60 p-3.5 rounded-lg flex gap-2.5 text-left text-[11px] text-on-surface-variant">
          <span className="material-symbols-outlined text-[16px] text-primary shrink-0 mt-0.5">info</span>
          <div>
            You may leave this page. We'll keep processing your order and notify you in your dashboard when it completes.
          </div>
        </div>
      </div>
    </div>
  );
};
