"use client";
import Image from "next/image";
import { useTheme } from "nextra-theme-docs";

export const StorageOverviewImage = () => {
  return (
    <div className="mt-4">
      {useTheme().resolvedTheme === "dark" ? (
        <Image
          className="rounded-lg"
          src="/image/kastrax-storage-overview-dark.png"
          alt="Diagram showing storage in Kastrax"
          width={700}
          height={700}
        />
      ) : (
        <Image
          className="rounded-lg"
          src="/image/kastrax-storage-overview-light.png"
          alt="Diagram showing storage in Kastrax"
          width={700}
          height={700}
        />
      )}
    </div>
  );
};
