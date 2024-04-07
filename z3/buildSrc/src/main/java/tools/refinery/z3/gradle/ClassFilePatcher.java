/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tools.refinery.z3.gradle;

import org.objectweb.asm.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class ClassFilePatcher {
	private ClassFilePatcher() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static void removeClassInitializer(File classFile) throws IOException {
		byte[] resultBytes;
		try (var fileReader = new FileInputStream(classFile)) {
			var classReader = new ClassReader(fileReader);
			var classWriter = new ClassWriter(classReader, 0);
			var classVisitor = new Visitor(classWriter);
			classReader.accept(classVisitor, 0);
			resultBytes = classWriter.toByteArray();
		}
		try (var fileWriter = new FileOutputStream(classFile)) {
			fileWriter.write(resultBytes);
		}
	}

	private static class Visitor extends ClassVisitor {
		protected Visitor(ClassVisitor classVisitor) {
			super(Opcodes.ASM9, classVisitor);
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
										 String[] exceptions) {
			if (name.equals("<clinit>")) {
				return null;
			}
			return super.visitMethod(access, name, descriptor, signature, exceptions);
		}
	}
}
