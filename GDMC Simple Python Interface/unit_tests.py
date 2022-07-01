from gdmc_interface import set_block, set_blocks, get_block, run_command
import unittest
import random
import math
import time
import sys

backup_stdout = sys.stdout

# return codes
return_code_ok = 200
return_code_bad_request = 400
return_code_precondition_required = 428


class TestGDMCInterface(unittest.TestCase):

    def test_block_manipulation(self):
        with self.subTest("Giving an invalid material"):
            request = set_block(0, 0, 0, "not_a_valid_material")
            expected_return_text = "Unknown material \"minecraft:not_a_valid_material\"."
            self.assertEqual(request.status_code, return_code_bad_request)
            self.assertEqual(request.text, expected_return_text)

            request = set_block(0, 0, 0, 1) # Trying to give a numeric id
            expected_return_text = "Unknown material \"minecraft:1\"."
            self.assertEqual(request.status_code, return_code_bad_request)
            self.assertEqual(request.text, expected_return_text)

        with self.subTest("Not giving a material"):
            request = set_block(0, 0, 0, None)
            expected_return_text = "Unknown material \"minecraft:none\"."
            self.assertEqual(request.status_code, return_code_bad_request)
            self.assertEqual(request.text, expected_return_text)

        with self.subTest("Using an invalid syntax"):
            bad_requests = [set_block("minecraft:stone", 0, 0, "minecraft:tnt"),
                        set_block(0, "~minecraft:stone", 0, "minecraft:tnt"),
                        set_block(0, None, 0, 0)]
            expected_return_text_prefix = "Could not parse query parameter"
            for index, request in enumerate(bad_requests):
                self.assertEqual(request.status_code, return_code_bad_request,
                                 msg=f'See bad request with index {index}')
                self.assertIn(expected_return_text_prefix, request.text, msg=f'See bad request with index {index}')

        with self.subTest("Checking actual block placement with #get_block()"):
            # Placing a block
            request = set_block(0, 0, 0, "diamond_block")
            expected_return_text = 'Done'
            self.assertEqual(request.status_code, return_code_ok)
            self.assertEqual(request.text, expected_return_text)

            # Then retrieving it
            request = get_block(0, 0, 0)
            expected_return_text = "minecraft:diamond_block"
            self.assertEqual(request.status_code, return_code_ok)
            self.assertEqual(request.text, expected_return_text)

    def test_batch_block_placement(self):
            X, Y, Z = (list(), list(), list())
            for x in range(4):
                for y in range(4):
                    for z in range(4):
                        X+=[x]
                        Y+=[y]
                        Z+=[z]
            # 64 blocks batch

            request = set_blocks(X, Y, Z, ["air" for _ in X])
            expected_return_text = 'Done'
            self.assertEqual(request.status_code, return_code_ok)
            self.assertEqual(request.text, expected_return_text)

    def test_block_placement_performance(self):
        time.sleep(0)
        print("", file=backup_stdout)
        for batch_size in [256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072]:
            block_count = 1000000
            X, Y, Z, materials = (list(), list(), list(), list())
            i = 0
            for batch in range(math.ceil(block_count/batch_size)):
                X_batch, Y_batch, Z_batch, m_batch = (list(), list(), list(), list())
                while i<batch*batch_size and i<block_count:
                    X_batch += [random.randint(0,15)]
                    Y_batch += [random.randint(0,15)]
                    Z_batch += [random.randint(0,15)]
                    m_batch += [random.choice(["stone", "cobblestone", "diorite", "andesite", "dirt"])]
                    i += 1
                X += [X_batch]
                Y += [Y_batch]
                Z += [Z_batch]
                materials += [m_batch]

            t0 = time.time()
            for X_batch, Y_batch, Z_batch, m_batch in zip(X, Y, Z, materials):
                request = set_blocks(X_batch, Y_batch, Z_batch, m_batch)
                expected_return_text = 'Done'
                self.assertEqual(request.status_code, return_code_ok)
                self.assertEqual(request.text, expected_return_text)
            print(f' #  Placing {block_count} blocks with a batch size of {batch_size} took {round(time.time()-t0,3)} '
                  'seconds', file=backup_stdout)

        print("Perf evaluation ... ", file=backup_stdout)

    def test_run_command(self):
        request = run_command("say Hello World!")
        self.assertEqual("", request.text)
        self.assertEqual(return_code_ok, request.status_code)

        request = run_command(run_command("BWAKBWAKBWAK"))
        self.assertEqual("Unknown command. Type \"/help\" for help.", request.text)
        self.assertEqual(return_code_ok, request.status_code)


if __name__ == '__main__':
    unittest.main(verbosity=2, exit=False)