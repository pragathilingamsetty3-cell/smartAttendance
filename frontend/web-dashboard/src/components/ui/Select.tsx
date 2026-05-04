'use client';

import React, { Fragment, useState } from 'react';
import { Combobox, Transition } from '@headlessui/react';
import { Check, ChevronDown, Search } from 'lucide-react';
import { cn } from '@/utils/cn';

export interface SelectOption {
  value: string | number;
  label: string;
}

interface SelectProps {
  name?: string;
  value: string | number;
  onChange: (value: string, name?: string) => void;
  options: SelectOption[];
  placeholder?: string;
  label?: string;
  disabled?: boolean;
  required?: boolean;
  creatable?: boolean;
  className?: string;
}

export const Select: React.FC<SelectProps> = ({
  name,
  value,
  onChange,
  options,
  placeholder = "Select an option",
  label,
  disabled = false,
  required = false,
  creatable = false,
  className
}) => {
  const [query, setQuery] = useState('');

  const selectedOption = options.find(opt => opt.value.toString() === value?.toString());

  const filteredOptions = query === ''
    ? options
    : options.filter((option) =>
        (option.label || (option as any).name || '')
          .toLowerCase()
          .replace(/\s+/g, '')
          .includes(query.toLowerCase().replace(/\s+/g, ''))
      );

  return (
    <div className={cn("relative w-full group", className)}>
      {label && (
        <label className="block text-sm font-medium text-slate-500 mb-2 group-focus-within:text-primary transition-colors">
          {label} {required && <span className="text-red-500">*</span>}
        </label>
      )}

      <Combobox
        value={value?.toString() || ""}
        onChange={(val: string | null) => {
          if (val !== null && val !== undefined) {
            onChange(val, name);
          }
        }}
        disabled={disabled}
        immediate
      >
        <div className="relative">
          <div className="relative">
            <Combobox.Input
              className={cn(
                "relative w-full cursor-text rounded-xl bg-slate-50 border border-slate-200 py-3.5 pl-4 pr-12 text-left text-slate-900 transition-all duration-300 hover:border-slate-300 focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary placeholder:text-slate-400",
                disabled && "opacity-60 cursor-not-allowed bg-slate-100"
              )}
              displayValue={() => (selectedOption?.label || (selectedOption as any)?.name || '')}
              onChange={(event) => setQuery(event.target.value)}
              onClick={(e) => {
                // Ensure it opens on click
                if (!disabled) {
                  const target = e.target as HTMLInputElement;
                  target.select();
                }
              }}
              placeholder={placeholder}
              autoComplete="off"
            />
            <Combobox.Button className="absolute inset-y-0 right-0 flex items-center pr-4">
              <ChevronDown className="h-5 w-5 text-slate-400 hover:text-slate-900 transition-colors" />
            </Combobox.Button>
          </div>

          <Transition
            as={Fragment}
            leave="transition ease-in duration-150"
            leaveFrom="opacity-100 translate-y-0"
            leaveTo="opacity-0 -translate-y-2"
            afterLeave={() => setQuery('')}
            enter="transition ease-out duration-200"
            enterFrom="opacity-0 -translate-y-2"
            enterTo="opacity-100 translate-y-0"
          >
            <Combobox.Options className="absolute z-[100] mt-2 max-h-60 w-full overflow-auto rounded-xl bg-white py-2 text-base shadow-xl shadow-sky-900/10 border border-slate-200 focus:outline-none sm:text-sm ring-1 ring-slate-100 backdrop-blur-xl">
              {filteredOptions.length === 0 && query !== '' ? (
                creatable ? (
                  <Combobox.Option
                    value={query}
                    className={({ active }) =>
                      cn(
                        "relative cursor-pointer select-none py-3 pl-11 pr-4 transition-all duration-200",
                        active ? "bg-primary/20 text-slate-900" : "text-slate-600"
                      )
                    }
                  >
                    Select "<span className="text-primary font-medium">{query}</span>"
                  </Combobox.Option>
                ) : (
                  <div className="relative cursor-default select-none py-4 px-4 text-gray-400 text-center italic">
                    No matching results for "{query}"
                  </div>
                )
              ) : filteredOptions.length === 0 ? (
                <div className="relative cursor-default select-none py-4 px-4 text-gray-400 text-center italic">
                  No options available
                </div>
              ) : (
                <>
                  {creatable && query !== '' && !filteredOptions.some(opt => opt.label.toLowerCase() === query.toLowerCase()) && (
                    <Combobox.Option
                      value={query}
                      className={({ active }) =>
                        cn(
                          "relative cursor-pointer select-none py-3 pl-11 pr-4 transition-all duration-200 border-b border-slate-100",
                          active ? "bg-primary/20 text-slate-900" : "text-slate-600"
                        )
                      }
                    >
                      Select "<span className="text-primary font-medium">{query}</span>"
                    </Combobox.Option>
                  )}
                  {filteredOptions.map((option) => (
                    <Combobox.Option
                      key={option.value.toString()}
                      className={({ active, selected }) =>
                        cn(
                          "relative cursor-pointer select-none py-3 pl-11 pr-4 transition-all duration-200 font-medium",
                          active ? "bg-primary/20 text-slate-900" : "text-slate-600",
                          selected ? "bg-primary/10" : ""
                        )
                      }
                      value={option.value.toString()}
                    >
                      {({ selected, active }) => (
                        <>
                          <span className={cn("block truncate transition-all", selected ? "font-semibold text-slate-900" : "font-normal")}>
                            {option.label || (option as any).name}
                          </span>
                          {selected ? (
                            <span className={cn(
                              "absolute inset-y-0 left-0 flex items-center pl-3",
                              active ? "text-slate-900" : "text-primary"
                            )}>
                              <Check className="h-4 w-4" />
                            </span>
                          ) : active ? (
                             <span className="absolute inset-y-0 left-0 flex items-center pl-3 text-primary opacity-20">
                               <Search className="h-4 w-4" />
                             </span>
                          ) : null}
                        </>
                      )}
                    </Combobox.Option>
                  ))}
                </>
              )}
            </Combobox.Options>
          </Transition>
        </div>
      </Combobox>
    </div>
  );
};


export default Select;
