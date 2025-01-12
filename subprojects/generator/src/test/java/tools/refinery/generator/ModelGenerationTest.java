package tools.refinery.generator;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.language.tests.InjectWithRefinery;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

@InjectWithRefinery
class ModelGenerationTest {

	@Inject
	ProblemLoader loader;
	@Inject
	ModelGeneratorFactory generatorFactory;


	@Test
	void modelGenerationTest() throws IOException{
		String inputPath = "C:\\Users\\freyd\\Desktop\\MSC_3\\Dipterv\\refinery\\subprojects\\generator\\src\\test" +
				"\\resources\\ASV3Vessel.problem";

		var problem = loader.loadFile(inputPath);
		generatorFactory.partialInterpretationBasedNeighborhoods(true);
		generatorFactory.keepNonExistingObjects(true);
		generatorFactory.keepShadowPredicates(true);

		var generator = generatorFactory.createGenerator(problem);
		generator.setRandomSeed(1);
		generator.setMaxNumberOfSolutions(5000);
		generator.generate();

		int solutionCount = generator.getSolutionCount();
		for (int i = 0; i < solutionCount; i++) {
			String outputPath = "functional_model" + i + ".problem";
			generator.loadSolution(i);
			var serializedModel = generator.serialize();
			var resource = serializedModel.eResource();
			var saveOptions = Map.of();
			try (var outputStream = new FileOutputStream(outputPath)) {
				resource.save(outputStream, saveOptions);
			}
		}
	}
}
