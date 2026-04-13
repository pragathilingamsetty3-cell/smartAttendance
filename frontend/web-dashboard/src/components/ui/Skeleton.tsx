import { cn } from "@/utils/cn";

export function Skeleton({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn("animate-skeleton rounded-lg bg-obsidian-800/80", className)}
      {...props}
    />
  );
}
